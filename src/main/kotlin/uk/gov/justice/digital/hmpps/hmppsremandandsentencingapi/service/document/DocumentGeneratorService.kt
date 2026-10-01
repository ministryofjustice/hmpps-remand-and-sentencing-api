package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document

import java.io.InputStream

class DocumentGeneratorService(
  val htmlRenderer: HtmlRenderer,
  val htmlToDocumentConverter: HtmlToDocumentConverter,
) {

  fun <T : DocumentDetail<*>> renderDocument(documentDetail: T): InputStream {
    val html = this.htmlRenderer.render(documentDetail)
    val doc = this.htmlToDocumentConverter.convertToStream(html)
    return doc
  }
}
