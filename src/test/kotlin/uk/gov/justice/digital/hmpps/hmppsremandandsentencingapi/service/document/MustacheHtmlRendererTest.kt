package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document

import org.assertj.core.api.Assertions.assertThat
import org.jsoup.Jsoup
import org.jsoup.helper.W3CDom
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document.impl.LodgeWarrants986
import java.io.File

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

    val lodgeWarrants986 = LodgeWarrants986(sampleData)
    val resp = convertTemplateToHtml(lodgeWarrants986)
    val html = W3CDom().asString(resp)

    val doc = Jsoup.parse(html)
    assertThat(doc.select("#name").text()).isEqualTo("Joe Bloggs")
    assertThat(doc.select("#nomsNumber").text()).isEqualTo("Joe Bloggs")
    assertThat(doc.select("#prisonNumber").text()).isEqualTo("Joe Bloggs")

    val outputDir = File("build/test-generated").apply { mkdirs() }
    File(outputDir, "sample-doc.html").writeBytes(html.toByteArray())
  }
}
