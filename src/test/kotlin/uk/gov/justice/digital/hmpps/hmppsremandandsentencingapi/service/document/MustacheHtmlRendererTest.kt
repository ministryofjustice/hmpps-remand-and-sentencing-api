package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document

import org.assertj.core.api.Assertions.assertThat
import org.jsoup.Jsoup
import org.jsoup.helper.W3CDom
import org.junit.jupiter.api.Test
import java.io.File
import java.time.LocalDate
import java.time.Period

class MustacheHtmlRendererTest {

  @Test
  fun `should render simple html from basic template and data`() {
    val courtCases1 = linkedMapOf<String, Any?>("courtName" to "Birmingham Crown Court")
    val courtCases2 = linkedMapOf<String, Any?>("courtName" to "Nottingham Crown Court")
    val data = linkedMapOf<String, Any?>("name" to "Joe", "surname" to "Bloggs", "courtCases" to listOf(courtCases1, courtCases2))
    val resp = convertTemplateToHtml(object : DocumentDetail<LinkedHashMap<String, Any?>> {
      override val templateName: String
        get() = "sample-doc.mustache"
      override val data: LinkedHashMap<String, Any?>
        get() = data
    })
    val doc = Jsoup.parse(W3CDom().asString(resp))
    assertThat(doc.select("h1").text()).isEqualTo("Prisoner Name: Joe Bloggs")
    assertThat(doc.select("li").eachText()).containsExactly(
      "Court Name Is: Birmingham Crown Court",
      "Court Name Is: Nottingham Crown Court",
    )
  }

  @Test
  fun `should render html from LodgeWarrants986`() {
    val sampleData = LodgeWarrants986.Data(
      name = "Joe Bloggs",
      nomsNumber = "AA4453",
      court = LodgeWarrants986.Court("Liverpool Crown Court", "The Queen Elizabeth II Law Courts", "Derby Square", "Liverpool", "Merseyside", "L2 1XA"),
      docGeneratedDate = LocalDate.parse("2026-09-29"),
      sentenceDate = LocalDate.parse("2026-09-17"),
      sentences = listOf(
        LodgeWarrants986.Sentence(Period.of(1, 2, 0), "Abandon a fighting dog", 100.0),
        LodgeWarrants986.Sentence(Period.of(0, 1, 0), "ASSAULT COURT/PRISON OFFICER", 50.0),
        LodgeWarrants986.Sentence(Period.of(1, 1, 8), "Loaf of bread stolen \uD83D\uDE32", 1000.0),
      ),
      telephoneNumber = "128 555 1719",
      prisonName = "KIRKHAM (HMP)",
      version = "3.23",
    )

    val lodgeWarrants986 = LodgeWarrants986(sampleData)
    val resp = convertTemplateToHtml(lodgeWarrants986)
    val html = W3CDom().asString(resp)

    val doc = Jsoup.parse(html)
    assertThat(doc.select("#name").text()).isEqualTo("Joe Bloggs")
    assertThat(doc.select("#nomsNumber").text()).isEqualTo("AA4453")

    val outputDir = File("build/test-generated").apply { mkdirs() }
    File(outputDir, "sample-doc.html").writeBytes(html.toByteArray())
  }
}
