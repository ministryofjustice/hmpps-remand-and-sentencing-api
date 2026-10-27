package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.document

import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.openssl.jcajce.JcaPEMWriter
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.StringWriter
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.util.Base64
import java.util.Date

/** Dummy self-signed identity generated on the fly, base64 encoded. */
class DocumentSigningCredentials {
  val cert: String
  val key: String

  init {
    val pair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
    val dn = X500Name("CN=remand-and-sentencing-api-test")
    val now = System.currentTimeMillis()
    val certificate = JcaX509CertificateConverter().getCertificate(
      JcaX509v3CertificateBuilder(
        dn,
        BigInteger.valueOf(now),
        Date(now - 60_000),
        Date(now + 86_400_000),
        dn,
        pair.public,
      )
        .build(JcaContentSignerBuilder("SHA256withRSA").build(pair.private)),
    )
    cert = encode(certificate)
    key = encode(pair.private)
  }

  private fun encode(obj: Any): String {
    val pem = StringWriter().also { writer -> JcaPEMWriter(writer).use { it.writeObject(obj) } }.toString()
    return Base64.getEncoder().encodeToString(pem.toByteArray())
  }
}
