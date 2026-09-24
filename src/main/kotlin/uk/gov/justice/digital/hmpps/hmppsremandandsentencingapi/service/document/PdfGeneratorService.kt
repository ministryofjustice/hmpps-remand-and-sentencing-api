package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document

import org.springframework.core.io.ClassPathResource
import java.io.InputStream

class PdfGeneratorService<T, U : DocumentDetail<T>>(
  val htmlRenderer: HtmlRenderer<T, U>,
  val htmlToPdfConverter: HtmlToPdfConverter,
) {

  fun renderServiceTemplate(documentDetail: U): InputStream {
    val template = ClassPathResource("templates/" + documentDetail.templateName).file.readText()
    val html = this.htmlRenderer.render(template, documentDetail.data)
    val doc = this.htmlToPdfConverter.convertToStream(html)
    return doc
  }
}
