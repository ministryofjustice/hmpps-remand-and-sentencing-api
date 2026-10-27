package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document.pdf

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions
import org.bouncycastle.asn1.ASN1EncodableVector
import org.bouncycastle.asn1.ASN1Primitive
import org.bouncycastle.asn1.DERSet
import org.bouncycastle.asn1.cms.Attribute
import org.bouncycastle.asn1.cms.AttributeTable
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers
import org.bouncycastle.cert.jcajce.JcaCertStore
import org.bouncycastle.cms.CMSProcessableByteArray
import org.bouncycastle.cms.CMSSignedData
import org.bouncycastle.cms.CMSSignedDataGenerator
import org.bouncycastle.cms.SignerInformation
import org.bouncycastle.cms.SignerInformationStore
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.cert.Certificate
import java.security.cert.X509Certificate
import java.util.Calendar

class PdfDigitalSignatureWriter(
  private val privateKey: PrivateKey? = null,
  private val chain: Array<Certificate>? = null,
  private val timestampAuthorityGenerator: TimestampAuthorityGenerator? = null,
) {

  fun apply(unsignedPdf: ByteArray): ByteArray {
    if (privateKey == null || chain.isNullOrEmpty()) {
      log.warn("Private key and certificates not specified, falling back to unsigned pdf.")
      return unsignedPdf
    }

    PDDocument.load(unsignedPdf).use { doc ->
      doc.documentInformation.apply {
        producer = "Ministry of Justice: HMPPS Digital Signature"
        cosObject.isNeedToBeUpdated = true
      }
      val signature = PDSignature().apply {
        setFilter(PDSignature.FILTER_ADOBE_PPKLITE)
        setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED)
        signDate = Calendar.getInstance()
      }
      val options = SignatureOptions().apply {
        if (timestampAuthorityGenerator != null) preferredSignatureSize = SignatureOptions.DEFAULT_SIGNATURE_SIZE * 2
      }
      doc.addSignature(signature, { content ->
        val generator = CMSSignedDataGenerator()
        val algorithm = if (privateKey.algorithm.equals("RSA", ignoreCase = true)) "SHA256withRSA" else "SHA256withECDSA"
        val contentSigner = JcaContentSignerBuilder(algorithm).build(privateKey)
        generator.addSignerInfoGenerator(
          JcaSignerInfoGeneratorBuilder(JcaDigestCalculatorProviderBuilder().build())
            .build(contentSigner, chain[0] as X509Certificate),
        )
        generator.addCertificates(JcaCertStore(chain.toList()))
        val signedPdf = generator.generate(CMSProcessableByteArray(content.readAllBytes()), false)
        timestampAuthorityGenerator?.let { addTimestamp(signedPdf, it) }?.encoded ?: signedPdf.encoded
      }, options)
      val out = ByteArrayOutputStream()
      doc.saveIncremental(out)
      return out.toByteArray()
    }
  }

  private fun addTimestamp(signedPdf: CMSSignedData, authority: TimestampAuthorityGenerator): CMSSignedData? = try {
    val signer = signedPdf.signerInfos.signers.first()
    val digest = MessageDigest.getInstance("SHA-256").digest(signer.signature)
    val token = ASN1Primitive.fromByteArray(authority.generateTimestamp(digest))
    val attributes = ASN1EncodableVector().apply {
      add(Attribute(PKCSObjectIdentifiers.id_aa_signatureTimeStampToken, DERSet(token)))
    }
    val stamped = SignerInformation.replaceUnsignedAttributes(signer, AttributeTable(attributes))
    CMSSignedData.replaceSigners(signedPdf, SignerInformationStore(stamped))
  } catch (e: Exception) {
    log.warn("Failed to obtain a timestamp, signing without one.", e)
    null
  }

  companion object {
    private val log = LoggerFactory.getLogger(PdfDigitalSignatureWriter::class.java)
  }
}
