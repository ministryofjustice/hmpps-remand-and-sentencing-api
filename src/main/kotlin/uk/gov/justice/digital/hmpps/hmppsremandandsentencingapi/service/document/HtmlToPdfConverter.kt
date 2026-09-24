package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document

import org.w3c.dom.Document
import java.io.ByteArrayInputStream
import java.io.InputStream

interface HtmlToPdfConverter {
  fun convert(html: Document): ByteArray
  fun convertToStream(html: Document): InputStream = ByteArrayInputStream(convert(html))
}
