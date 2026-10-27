package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document.pdf

import io.mockk.every
import io.mockk.mockk
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.assertj.core.api.Assertions.assertThat
import org.bouncycastle.asn1.ASN1InputStream
import org.bouncycastle.asn1.ASN1ObjectIdentifier
import org.bouncycastle.asn1.cms.ContentInfo
import org.bouncycastle.asn1.nist.NISTObjectIdentifiers
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.AlgorithmIdentifier
import org.bouncycastle.asn1.x509.ExtendedKeyUsage
import org.bouncycastle.asn1.x509.Extension
import org.bouncycastle.asn1.x509.KeyPurposeId
import org.bouncycastle.cert.jcajce.JcaCertStore
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.cms.CMSProcessableByteArray
import org.bouncycastle.cms.CMSSignedData
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoGeneratorBuilder
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder
import org.bouncycastle.tsp.TSPAlgorithms
import org.bouncycastle.tsp.TimeStampRequestGenerator
import org.bouncycastle.tsp.TimeStampResponseGenerator
import org.bouncycastle.tsp.TimeStampTokenGenerator
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.Security
import java.security.cert.X509Certificate
import java.util.Date

class PdfDigitalSignatureWriterTimestampTest {

  @Test
  fun `should embed a valid timestamp token over the signature value`() {
    val requestedDigests = mutableListOf<ByteArray>()
    val maker = PdfDigitalSignatureWriter(signer.key, arrayOf(signer.cert), fakeTsa(requestedDigests))

    val signedPdf = maker.apply(blankPdf())

    val signerInfo = signerInfoOf(signedPdf)
    val tokenAttribute = signerInfo.unsignedAttributes[PKCSObjectIdentifiers.id_aa_signatureTimeStampToken]
    assertThat(tokenAttribute).isNotNull()
    assertThat(requestedDigests).hasSize(1)
    assertThat(requestedDigests.single()).isEqualTo(MessageDigest.getInstance("SHA-256").digest(signerInfo.signature))
    assertThat(signerInfo.verify(JcaSimpleSignerInfoVerifierBuilder().build(signer.cert))).isTrue()
  }

  @Test
  fun `should sign without a timestamp when the authority fails`() {
    val maker = PdfDigitalSignatureWriter(signer.key, arrayOf(signer.cert), mockk<TimestampAuthorityGenerator> { every { generateTimestamp(any()) } throws IllegalStateException("TSA down") })

    val signerInfo = signerInfoOf(maker.apply(blankPdf()))

    assertThat(signerInfo.unsignedAttributes).isNull()
    assertThat(signerInfo.verify(JcaSimpleSignerInfoVerifierBuilder().build(signer.cert))).isTrue()
  }

  @Test
  fun `should sign without a timestamp when no authority is configured`() {
    val signerInfo = signerInfoOf(PdfDigitalSignatureWriter(signer.key, arrayOf(signer.cert)).apply(blankPdf()))

    assertThat(signerInfo.unsignedAttributes).isNull()
  }

  private fun signerInfoOf(pdf: ByteArray) = PDDocument.load(pdf).use { doc ->
    val sig = doc.signatureDictionaries.single()
    val contentInfo = ASN1InputStream(sig.getContents(pdf)).use { ContentInfo.getInstance(it.readObject()) }
    CMSSignedData(CMSProcessableByteArray(sig.getSignedContent(pdf)), contentInfo).signerInfos.signers.first()
  }

  private fun blankPdf(): ByteArray = PDDocument().use { doc ->
    doc.addPage(PDPage())
    ByteArrayOutputStream().also { doc.save(it) }.toByteArray()
  }

  private fun fakeTsa(requestedDigests: MutableList<ByteArray>) = mockk<TimestampAuthorityGenerator> {
    every { generateTimestamp(any()) } answers { issueToken(firstArg<ByteArray>().also { requestedDigests.add(it) }) }
  }

  private fun issueToken(digest: ByteArray): ByteArray {
    val tokenGenerator = TimeStampTokenGenerator(
      JcaSimpleSignerInfoGeneratorBuilder().build("SHA256withRSA", tsa.key, tsa.cert),
      JcaDigestCalculatorProviderBuilder().build().get(AlgorithmIdentifier(NISTObjectIdentifiers.id_sha256)),
      ASN1ObjectIdentifier("1.2.3.4.5"),
    ).apply { addCertificates(JcaCertStore(listOf(tsa.cert))) }
    val request = TimeStampRequestGenerator().apply { setCertReq(true) }
      .generate(TSPAlgorithms.SHA256, digest, BigInteger.ONE)
    return TimeStampResponseGenerator(tokenGenerator, TSPAlgorithms.ALLOWED)
      .generate(request, BigInteger.ONE, Date())
      .timeStampToken.encoded
  }

  private class Identity(val key: java.security.PrivateKey, val cert: X509Certificate)

  companion object {
    private lateinit var signer: Identity
    private lateinit var tsa: Identity

    @JvmStatic
    @BeforeAll
    fun setUp() {
      Security.addProvider(BouncyCastleProvider())
      signer = identity("CN=signer", timestamping = false)
      tsa = identity("CN=tsa", timestamping = true)
    }

    private fun identity(dn: String, timestamping: Boolean): Identity {
      val pair: KeyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
      val now = System.currentTimeMillis()
      val builder = JcaX509v3CertificateBuilder(X500Name(dn), BigInteger.valueOf(now), Date(now - 60_000), Date(now + 3_600_000), X500Name(dn), pair.public)
      if (timestamping) {
        builder.addExtension(Extension.extendedKeyUsage, true, ExtendedKeyUsage(KeyPurposeId.id_kp_timeStamping))
      }
      val cert = JcaX509CertificateConverter().getCertificate(builder.build(JcaContentSignerBuilder("SHA256withRSA").build(pair.private)))
      return Identity(pair.private, cert)
    }
  }
}
