package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document

import java.io.InputStream

class DocumentGeneratorService<T : DocumentDetail<*>>(
  val htmlRenderer: HtmlRenderer<T>,
  val htmlToDocumentConverter: HtmlToDocumentConverter,
) {

  fun renderDocument(documentDetail: T): InputStream {
    val html = this.htmlRenderer.render(documentDetail)
    val doc = this.htmlToDocumentConverter.convertToStream(html)
    return doc
  }
}
