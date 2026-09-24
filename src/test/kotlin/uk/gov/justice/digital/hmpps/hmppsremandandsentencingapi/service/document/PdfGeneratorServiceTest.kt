package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document.impl.LodgeWarrants986
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document.impl.MustacheHtmlRenderer
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document.impl.OpenHtmlToPdfConverter
import java.io.File

class PdfGeneratorServiceTest {

  private fun sampleData() = LodgeWarrants986.Data(
    name = "Joe Bloggs",
    nomsNumber = "AA4453",
    prisonNumber = "993453",
    courtName = "Airdrie Sheriff Court",
    date = "17/09/2026",
    sentences = listOf(
      LodgeWarrants986.Sentence("0 Years 1 Month 0 Day", "Abandon a fighting dog"),
      LodgeWarrants986.Sentence("0 Years 1 Month 0 Day", "ASSAULT COURT/PRISON OFFICER"),
    ),
    telephoneNumber = "128 555 1719",
    prisonName = "KIRKHAM (HMP)",
  )

  @Test
  fun `should render pdf from from LodgeWarrants986`() {
    val htmlRenderer = MustacheHtmlRenderer<LodgeWarrants986>()
    val pdfConverter = OpenHtmlToPdfConverter()
    val lodgeWarrants986 = LodgeWarrants986(sampleData())
    val pdfGeneratorService = PdfGeneratorService(htmlRenderer, pdfConverter)

    val resp = pdfGeneratorService.renderServiceTemplate(lodgeWarrants986)

    PDDocument.load(resp).use { pdf ->
      assertThat(pdf.numberOfPages).isGreaterThan(0)
      val text = PDFTextStripper().getText(pdf)
      assertThat(text).contains("RESTRICTED")
      assertThat(text).contains("RULE 63(1) MAGISTRATES' COURTS RULES 1981")
      assertThat(text).contains("Name")
      assertThat(text).contains("Joe Bloggs")
      assertThat(text).contains("NOMS No")
      assertThat(text).contains("AA4453")
      assertThat(text).contains("Prison No")
      assertThat(text).contains("993453")
      assertThat(text).contains("Airdrie Sheriff Court")
      assertThat(text).contains("17/09/2026")
      assertThat(text).contains("PART A")
      assertThat(text).contains("Abandon a fighting dog")
      assertThat(text).contains("ASSAULT COURT/PRISON OFFICER")
      assertThat(text).contains("TOTAL")
      assertThat(text).contains("PART B")
      assertThat(text).contains("128 555 1719")
      assertThat(text).contains("KIRKHAM (HMP)")
    }
  }

  @Test
  fun `should save pdf from from LodgeWarrants986`() {
    val htmlRenderer = MustacheHtmlRenderer<LodgeWarrants986>()
    val pdfConverter = OpenHtmlToPdfConverter()
    val lodgeWarrants986 = LodgeWarrants986(sampleData())
    val pdfGeneratorService = PdfGeneratorService(htmlRenderer, pdfConverter)

    val resp = pdfGeneratorService.renderServiceTemplate(lodgeWarrants986)

    val outputDir = File("build/test-generated").apply { mkdirs() }
    File(outputDir, "sample-doc.pdf").writeBytes(resp.readAllBytes())
  }
}
