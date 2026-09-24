package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document

import java.io.InputStream

class PdfGeneratorService<T : DocumentDetail<*>>(
  val htmlRenderer: HtmlRenderer<T>,
  val htmlToPdfConverter: HtmlToPdfConverter,
) {

  fun renderServiceTemplate(documentDetail: T): InputStream {
    val html = this.htmlRenderer.render(documentDetail)
    val doc = this.htmlToPdfConverter.convertToStream(html)
    return doc
  }
}
