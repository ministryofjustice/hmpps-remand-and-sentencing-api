package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PdfGeneratorServiceTest {

  @Test
  fun `should render pdf from from LodgeWarrants986`() {
    val htmlRenderer = MustacheHtmlRenderer<LodgeWarrants986.Data, LodgeWarrants986>()
    val pdfConverter = OpenHtmlToPdfConverter()
    val lodgeWarrants986 = LodgeWarrants986(LodgeWarrants986.Data("Joe Bloggs", "AA4453", "993453"))
    val pdfGeneratorService = PdfGeneratorService(htmlRenderer, pdfConverter)

    val resp = pdfGeneratorService.renderServiceTemplate(lodgeWarrants986)

    PDDocument.load(resp).use { pdf ->
      assertThat(pdf.numberOfPages).isGreaterThan(0)
      val text = PDFTextStripper().getText(pdf)
      assertThat(text).contains("Joe Bloggs")
      assertThat(text).contains("AA4453")
      assertThat(text).contains("993453")
    }
  }
}
