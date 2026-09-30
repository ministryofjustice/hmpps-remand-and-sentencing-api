package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document.dto.PeriodLength
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document.mustache.MustacheHtmlRenderer
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document.pdf.OpenHtmlToPdfConverter
import java.io.File
import java.time.LocalDate

class DocumentGeneratorServiceTest {

  private fun sampleData() = LodgeWarrants986.Data(
    name = "Joe Bloggs",
    nomsNumber = "AA4453",
    court = LodgeWarrants986.Court("Liverpool Crown Court", "The Queen Elizabeth II Law Courts", "Derby Square", "Liverpool", "Merseyside", "L2 1XA"),
    docGeneratedDate = LocalDate.parse("2026-09-29"),
    sentenceDate = LocalDate.parse("2026-09-17"),
    sentences = listOf(
      LodgeWarrants986.Sentence(listOf(PeriodLength(0, 1, 0)), "Abandon a fighting dog", 0.0),
      LodgeWarrants986.Sentence(listOf(PeriodLength(0, 1, 0)), "ASSAULT COURT/PRISON OFFICER", 0.0),
    ),
    telephoneNumber = "128 555 1719",
    prisonName = "KIRKHAM (HMP)",
    version = "3.23",
  )

  @Test
  fun `should render pdf from from LodgeWarrants986`() {
    val htmlRenderer: HtmlRenderer<LodgeWarrants986> = MustacheHtmlRenderer()
    val pdfConverter: HtmlToDocumentConverter = OpenHtmlToPdfConverter()
    val lodgeWarrants986 = LodgeWarrants986(sampleData())
    val pdfGeneratorService = DocumentGeneratorService(htmlRenderer, pdfConverter)

    val resp = pdfGeneratorService.renderDocument(lodgeWarrants986)

    PDDocument.load(resp).use { pdf ->
      assertThat(pdf.numberOfPages).isGreaterThan(0)
      val text = PDFTextStripper().getText(pdf)
      assertThat(text).contains("RESTRICTED")
      assertThat(text).contains("RULE 63(1) MAGISTRATES' COURTS RULES 1981")
      assertThat(text).contains("Name")
      assertThat(text).contains("Joe Bloggs")
      assertThat(text).contains("NOMS No")
      assertThat(text).contains("AA4453")
      assertThat(text).contains("Liverpool Crown Court")
      assertThat(text).contains("17/09/2026")
      assertThat(text).contains("PART A")
      assertThat(text).contains("Abandon a fighting dog")
      assertThat(text).contains("ASSAULT COURT/PRISON OFFICER")
      assertThat(text).contains("TOTAL")
      assertThat(text).contains("PART B")
      assertThat(text).contains("128 555 1719")
      assertThat(text).contains("KIRKHAM (HMP)")
    }

    val outputDir = File("build/test-generated").apply { mkdirs() }
    File(outputDir, "lodge-warrants-sample.pdf").writeBytes(resp.readAllBytes())
  }
}
