package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document.impl

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document.HtmlToPdfConverter
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
