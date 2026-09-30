package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document.pdf

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder
import org.w3c.dom.Document
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document.HtmlToDocumentConverter
import java.io.ByteArrayOutputStream

class OpenHtmlToPdfConverter : HtmlToDocumentConverter {

  override fun convert(document: Document): ByteArray {
    val contentOutputStream = ByteArrayOutputStream()

    PdfRendererBuilder()
      .useFastMode()
      .withW3cDocument(document, "")
      .toStream(contentOutputStream)
      .run()

    return contentOutputStream.toByteArray()
  }
}
