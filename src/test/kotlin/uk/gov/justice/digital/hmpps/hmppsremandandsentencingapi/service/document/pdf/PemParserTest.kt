package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document.pdf

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.cert.X509v3CertificateBuilder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.openssl.jcajce.JcaPEMWriter
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.StringWriter
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.cert.X509Certificate
import java.util.Base64
import java.util.Date

class PemParserTest {

  private val pemParser = PemParser()

  @Test
  fun `should parse standard pem`() {
    assertParses(certPem, pkcs1KeyPem)
  }

  @Test
  fun `should parse pkcs8 key`() {
    assertParses(certPem, pkcs8KeyPem)
  }

  @Test
  fun `should parse pem with newlines collapsed to spaces`() {
    assertParses(certPem.replace("\n", " ").trim(), pkcs1KeyPem.replace("\n", " ").trim())
  }

  @Test
  fun `should parse pem with escaped newlines`() {
    assertParses(certPem.replace("\n", "\\n"), pkcs1KeyPem.replace("\n", "\\n"))
  }

  @Test
  fun `should parse base64 encoded pem`() {
    assertParses(encodeB64(certPem), encodeB64(pkcs1KeyPem))
  }

  @Test
  fun `should parse certificate chain with multiple certificates`() {
    val chain = pemParser.parseCertificateChain(certPem + certPem)
    assertThat(chain).hasSize(2)
  }

  @Test
  fun `should reject pem without a private key`() {
    assertThatThrownBy { pemParser.parsePrivateKey(certPem) }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessageContaining("No supported private key")
  }

  @Test
  fun `should reject pem without a certificate`() {
    assertThatThrownBy { pemParser.parseCertificateChain(pkcs1KeyPem) }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessageContaining("No certificates found")
  }

  private fun assertParses(certInput: String, keyInput: String) {
    val key = pemParser.parsePrivateKey(keyInput)
    val chain = pemParser.parseCertificateChain(certInput)
    assertThat(key.algorithm).isEqualTo("RSA")
    assertThat((chain.first() as X509Certificate).subjectX500Principal.name).isEqualTo("CN=test")
  }

  private fun encodeB64(value: String) = Base64.getEncoder().encodeToString(value.toByteArray())

  companion object {
    private lateinit var certPem: String
    private lateinit var pkcs1KeyPem: String
    private lateinit var pkcs8KeyPem: String

    @JvmStatic
    @BeforeAll
    fun generate() {
      val keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
      val name = X500Name("CN=test")
      val now = System.currentTimeMillis()
      val holder = X509v3CertificateBuilder(
        name,
        BigInteger.ONE,
        Date(now - 60000),
        Date(now + 3600000),
        name,
        SubjectPublicKeyInfo.getInstance(keyPair.public.encoded),
      ).build(JcaContentSignerBuilder("SHA256withRSA").build(keyPair.private))
      val cert = JcaX509CertificateConverter().getCertificate(holder)
      certPem = pem(cert)
      pkcs8KeyPem = "-----BEGIN PRIVATE KEY-----\n" +
        Base64.getMimeEncoder(64, "\n".toByteArray()).encodeToString(keyPair.private.encoded) +
        "\n-----END PRIVATE KEY-----\n"
      // PKCS#1 ("BEGIN RSA PRIVATE KEY") format stored in k8s secrets
      pkcs1KeyPem = pem(PrivateKeyInfo.getInstance(keyPair.private.encoded))
      // Ensure the BouncyCastle's JcaPEMWriter writes the PrivateKeyInfo as PKCS#1
      // Note: This could break if the library is upgraded so we need to keep this check here
      check(pkcs1KeyPem.startsWith("-----BEGIN RSA PRIVATE KEY-----"))
    }

    private fun pem(obj: Any): String = StringWriter().also { w -> JcaPEMWriter(w).use { it.writeObject(obj) } }.toString()
  }
}
