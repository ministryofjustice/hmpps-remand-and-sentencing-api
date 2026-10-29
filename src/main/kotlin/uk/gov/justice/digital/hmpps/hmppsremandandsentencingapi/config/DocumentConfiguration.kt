package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.config

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document.DocumentGeneratorService
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document.HtmlRenderer
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document.HtmlToDocumentConverter
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document.mustache.MustacheHtmlRenderer
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document.pdf.OpenHtmlToPdfConverter
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document.pdf.PdfDigitalSignatureWriter
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document.pdf.PemParser
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document.pdf.TimestampAuthorityGenerator

@Configuration
class DocumentConfiguration {

  @Bean
  fun htmlRenderer(): HtmlRenderer = MustacheHtmlRenderer()

  @Bean
  fun htmlToDocumentConverter(pdfDigitalSignatureWriter: PdfDigitalSignatureWriter): HtmlToDocumentConverter = OpenHtmlToPdfConverter(pdfDigitalSignatureWriter)

  @Bean
  fun documentGeneratorService(
    htmlRenderer: HtmlRenderer,
    converter: HtmlToDocumentConverter,
  ): DocumentGeneratorService = DocumentGeneratorService(htmlRenderer, converter)

  @Bean
  fun pdfDigitalSignatureWriter(
    @Value("#{environment['document.signing.cert'] ?: null}") certPem: String?,
    @Value("#{environment['document.signing.key'] ?: null}") keyPem: String?,
    @Value("#{environment['document.signing.tsa-url'] ?: null}") tsaUrl: String?,
    @Qualifier("timestampAuthorityWebClient") timestampAuthorityWebClient: WebClient,
  ): PdfDigitalSignatureWriter {
    if (certPem.isNullOrBlank() || keyPem.isNullOrBlank()) {
      return PdfDigitalSignatureWriter()
    }
    val pemParser = PemParser()
    val tsa = tsaUrl?.takeIf { it.isNotBlank() }?.let { TimestampAuthorityGenerator(timestampAuthorityWebClient) }
    return PdfDigitalSignatureWriter(pemParser.parsePrivateKey(keyPem), pemParser.parseCertificateChain(certPem), tsa)
  }
}
