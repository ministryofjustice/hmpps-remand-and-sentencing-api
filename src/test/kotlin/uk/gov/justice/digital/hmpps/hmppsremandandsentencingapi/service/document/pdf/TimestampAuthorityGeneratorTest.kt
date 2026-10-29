package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document.pdf

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.bouncycastle.asn1.ASN1ObjectIdentifier
import org.bouncycastle.asn1.nist.NISTObjectIdentifiers
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.AlgorithmIdentifier
import org.bouncycastle.asn1.x509.ExtendedKeyUsage
import org.bouncycastle.asn1.x509.Extension
import org.bouncycastle.asn1.x509.KeyPurposeId
import org.bouncycastle.cert.jcajce.JcaCertStore
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.cms.CMSSignedData
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoGeneratorBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder
import org.bouncycastle.tsp.TSPAlgorithms
import org.bouncycastle.tsp.TimeStampRequest
import org.bouncycastle.tsp.TimeStampRequestGenerator
import org.bouncycastle.tsp.TimeStampResponseGenerator
import org.bouncycastle.tsp.TimeStampToken
import org.bouncycastle.tsp.TimeStampTokenGenerator
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.mock.http.client.reactive.MockClientHttpRequest
import org.springframework.web.reactive.function.BodyInserter
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.Security
import java.security.cert.X509Certificate
import java.time.Duration
import java.util.Date
import java.util.Optional

class TimestampAuthorityGeneratorTest {

  private var respondWith: (TimeStampRequest) -> Mono<ClientResponse> = { Mono.error(IllegalStateException("no stub")) }
  private var receivedRequest: ClientRequest? = null
  private var receivedTimeStampRequest: TimeStampRequest? = null

  private val digest = MessageDigest.getInstance("SHA-256").digest("dummy-signature".toByteArray())

  @Test
  fun `should post a timestamp query for the digest and return the token`() {
    respondWith = { ok(grantedResponse(it)) }

    val tokenBytes = generator().generateTimestamp(digest)

    assertThat(receivedRequest!!.method()).isEqualTo(HttpMethod.POST)
    assertThat(receivedRequest!!.headers().contentType).isEqualTo(MediaType.parseMediaType("application/timestamp-query"))
    assertThat(receivedTimeStampRequest!!.messageImprintAlgOID).isEqualTo(TSPAlgorithms.SHA256)
    assertThat(receivedTimeStampRequest!!.messageImprintDigest).isEqualTo(digest)
    assertThat(receivedTimeStampRequest!!.certReq).isTrue()
    assertThat(receivedTimeStampRequest!!.nonce).isNotNull()
    assertThat(TimeStampToken(CMSSignedData(tokenBytes)).timeStampInfo.messageImprintDigest).isEqualTo(digest)
  }

  @Test
  fun `should use a different nonce for each request`() {
    val nonces = mutableListOf<BigInteger>()
    respondWith = {
      nonces.add(it.nonce)
      ok(grantedResponse(it))
    }

    generator().generateTimestamp(digest)
    generator().generateTimestamp(digest)

    assertThat(nonces).hasSize(2)
    assertThat(nonces[0]).isNotEqualTo(nonces[1])
  }

  @Test
  fun `should fail when the response nonce does not match the request`() {
    respondWith = {
      val otherRequest = TimeStampRequestGenerator().apply { setCertReq(true) }
        .generate(TSPAlgorithms.SHA256, digest, it.nonce.add(BigInteger.ONE))
      ok(grantedResponse(otherRequest))
    }

    assertThatThrownBy { generator().generateTimestamp(digest) }
      .hasMessageContaining("nonce")
  }

  @Test
  fun `should fail when the authority returns a non 200 status`() {
    respondWith = { Mono.just(ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR).build()) }

    assertThatThrownBy { generator().generateTimestamp(digest) }
      .isInstanceOf(IllegalStateException::class.java)
      .hasMessage("Timestamp authority returned HTTP 500")
  }

  @Test
  fun `should fail when the authority rejects the request`() {
    respondWith = {
      ok(TimeStampResponseGenerator(tokenGenerator(), TSPAlgorithms.ALLOWED).generateRejectedResponse(Exception("nope")).encoded)
    }

    assertThatThrownBy { generator().generateTimestamp(digest) }
      .isInstanceOf(Exception::class.java)
  }

  @Test
  fun `should fail when the authority does not respond within the timeout`() {
    respondWith = { Mono.never() }

    assertThatThrownBy { generator(Duration.ofMillis(100)).generateTimestamp(digest) }
      .isInstanceOf(IllegalStateException::class.java)
  }

  private fun generator(timeout: Duration = Duration.ofSeconds(10)) = TimestampAuthorityGenerator(
    WebClient.builder().exchangeFunction { request ->
      receivedRequest = request
      val parsed = TimeStampRequest(readBody(request))
      receivedTimeStampRequest = parsed
      respondWith(parsed)
    }.build(),
    timeout,
  )

  private fun readBody(request: ClientRequest): ByteArray {
    val outputMessage = MockClientHttpRequest(request.method(), request.url())
    val context = object : BodyInserter.Context {
      override fun messageWriters() = ExchangeStrategies.withDefaults().messageWriters()
      override fun serverRequest() = Optional.empty<ServerHttpRequest>()
      override fun hints() = emptyMap<String, Any>()
    }
    request.body().insert(outputMessage, context).block()
    val buffer = DataBufferUtils.join(outputMessage.body).block()!!
    return ByteArray(buffer.readableByteCount()).also { buffer.read(it) }
  }

  private fun ok(body: ByteArray): Mono<ClientResponse> = Mono.just(
    ClientResponse.create(HttpStatus.OK).body(Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(body))).build(),
  )

  private fun grantedResponse(request: TimeStampRequest): ByteArray = TimeStampResponseGenerator(tokenGenerator(), TSPAlgorithms.ALLOWED)
    .generate(request, BigInteger.ONE, Date())
    .encoded

  private fun tokenGenerator() = TimeStampTokenGenerator(
    JcaSimpleSignerInfoGeneratorBuilder().build("SHA256withRSA", tsa.key, tsa.cert),
    JcaDigestCalculatorProviderBuilder().build().get(AlgorithmIdentifier(NISTObjectIdentifiers.id_sha256)),
    ASN1ObjectIdentifier("1.2.3.4.5"),
  ).apply { addCertificates(JcaCertStore(listOf(tsa.cert))) }

  private class Identity(val key: java.security.PrivateKey, val cert: X509Certificate)

  companion object {
    private lateinit var tsa: Identity

    @JvmStatic
    @BeforeAll
    fun setUp() {
      Security.addProvider(BouncyCastleProvider())
      val pair: KeyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
      val now = System.currentTimeMillis()
      val dn = X500Name("CN=tsa")
      val builder = JcaX509v3CertificateBuilder(dn, BigInteger.valueOf(now), Date(now - 60_000), Date(now + 3_600_000), dn, pair.public)
        .addExtension(Extension.extendedKeyUsage, true, ExtendedKeyUsage(KeyPurposeId.id_kp_timeStamping))
      val cert = JcaX509CertificateConverter().getCertificate(builder.build(JcaContentSignerBuilder("SHA256withRSA").build(pair.private)))
      tsa = Identity(pair.private, cert)
    }
  }
}
