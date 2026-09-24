package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream

@Service
class OpenHtmlToPdfConverter : HtmlToPdfConverter {

  override fun convert(html: String): ByteArray {
    val contentOutputStream = ByteArrayOutputStream()

    PdfRendererBuilder()
      .useFastMode()
      .withHtmlContent(html, null)
      .toStream(contentOutputStream)
      .run()

    return contentOutputStream.toByteArray()
  }
}
