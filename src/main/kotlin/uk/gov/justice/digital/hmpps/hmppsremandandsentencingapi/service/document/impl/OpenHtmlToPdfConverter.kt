package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document.impl

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder
import org.springframework.stereotype.Service
import org.w3c.dom.Document
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document.HtmlToPdfConverter
import java.io.ByteArrayOutputStream

@Service
class OpenHtmlToPdfConverter : HtmlToPdfConverter {

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
