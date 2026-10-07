package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document.DocumentGeneratorService
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document.HtmlRenderer
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document.HtmlToDocumentConverter
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document.mustache.MustacheHtmlRenderer
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document.pdf.OpenHtmlToPdfConverter

@Configuration
class DocumentConfiguration {

  @Bean
  fun htmlRenderer(): HtmlRenderer = MustacheHtmlRenderer()

  @Bean
  fun htmlToDocumentConverter(): HtmlToDocumentConverter = OpenHtmlToPdfConverter()

  @Bean
  fun documentGeneratorService(): DocumentGeneratorService = DocumentGeneratorService(htmlRenderer(), htmlToDocumentConverter())
}
