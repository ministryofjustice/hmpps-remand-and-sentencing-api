package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document.pdf

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo
import org.bouncycastle.cert.X509CertificateHolder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.openssl.PEMKeyPair
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import java.io.StringReader
import java.security.PrivateKey
import java.security.cert.Certificate
import java.util.Base64

class PemParser {
  private val pemBlock = Regex("-----BEGIN ([A-Z0-9 ]+)-----(.*?)-----END \\1-----", RegexOption.DOT_MATCHES_ALL)
  private val whitespace = Regex("\\s")

  fun parseCertificateChain(pem: String): Array<Certificate> {
    val converter = JcaX509CertificateConverter()
    val certs = mutableListOf<Certificate>()
    PEMParser(StringReader(unwrapOrSelf(pem))).use { parser ->
      while (true) {
        val certHolder = parser.readObject() ?: break
        if (certHolder is X509CertificateHolder) certs.add(converter.getCertificate(certHolder))
      }
    }
    require(certs.isNotEmpty()) { "No certificates found in PEM (length=${pem.length})" }
    return certs.toTypedArray()
  }

  fun parsePrivateKey(pem: String): PrivateKey {
    val converter = JcaPEMKeyConverter()
    val found = mutableListOf<String>()
    PEMParser(StringReader(unwrapOrSelf(pem))).use { parser ->
      while (true) {
        val obj = parser.readObject() ?: break
        when (obj) {
          is PEMKeyPair -> return converter.getKeyPair(obj).private
          is PrivateKeyInfo -> return converter.getPrivateKey(obj)
          else -> found.add(obj.javaClass.simpleName)
        }
      }
    }
    throw IllegalArgumentException("No supported private key in PEM (found: $found, length=${pem.length})")
  }

  private fun unwrapOrSelf(raw: String): String {
    val pem = if (raw.contains("-----BEGIN")) raw else decodeBase64OrSelf(raw)
    return pemBlock.findAll(pem).joinToString("") { match ->
      val type = match.groupValues[1]
      val body = match.groupValues[2].replace("\\n", "").replace(whitespace, "")
      "-----BEGIN $type-----\n${body.chunked(64).joinToString("\n")}\n-----END $type-----\n"
    }
  }

  private fun decodeBase64OrSelf(raw: String): String = runCatching {
    String(Base64.getMimeDecoder().decode(raw.trim()), Charsets.UTF_8)
  }.getOrDefault(raw)
}
