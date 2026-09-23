package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document

import java.io.ByteArrayInputStream
import java.io.InputStream

interface HtmlToPdfConverter {
  fun convert(html: String): ByteArray
  fun convertToStream(html: String): InputStream {
    return ByteArrayInputStream(convert(html))
  }
}