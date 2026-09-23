package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document

import java.io.InputStream

class PdfGeneratorService<T>(val htmlRenderer: HtmlRenderer<T>,
                          val htmlToPdfConverter: HtmlToPdfConverter) {

  fun renderServiceTemplate(documentDetail: DocumentDetail<T>) : InputStream {
    val html = this.htmlRenderer.render(documentDetail.getTemplateName(), documentDetail.getData())
    val doc = this.htmlToPdfConverter.convertToStream(html)
    return doc
  }

}