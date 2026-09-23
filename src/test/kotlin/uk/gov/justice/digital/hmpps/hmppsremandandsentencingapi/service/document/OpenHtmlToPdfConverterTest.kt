package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.File

class OpenHtmlToPdfConverterTest {

  @Test
  fun `should render simple html from basic template and data`() {
    val courtCases1 = linkedMapOf<String, Any?>("courtName" to "Birmingham Crown Court")
    val courtCases2 = linkedMapOf<String, Any?>("courtName" to "Nottingham Crown Court")
    val data = linkedMapOf<String, Any?>("name" to "Joe", "surname" to "Bloggs", "courtCases" to listOf(courtCases1, courtCases2))
    val html = convertTemplateToHtml("sample-doc.mustache", data)

    val pdfConverter = OpenHtmlToPdfConverter()
    var resp = pdfConverter.convert(html)

    PDDocument.load(resp).use { pdf ->
      assertThat(pdf.numberOfPages).isGreaterThan(0)
      val text = PDFTextStripper().getText(pdf)
      assertThat(text).contains("Joe Bloggs")
      assertThat(text).contains("Birmingham Crown Court")
    }

    // Output to build folder to check
    val outputDir = File("build/test-generated").apply { mkdirs() }
    File(outputDir, "sample-doc.pdf").writeBytes(resp)
  }

}