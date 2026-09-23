package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document

import org.assertj.core.api.Assertions.assertThat
import org.jsoup.Jsoup
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource

class MustacheHtmlRendererTest {

  @Test
  fun `should render simple html from basic template and data`() {
    val courtCases1 = linkedMapOf<String, Any?>("courtName" to "Birmingham Crown Court")
    val courtCases2 = linkedMapOf<String, Any?>("courtName" to "Nottingham Crown Court")
    val data = linkedMapOf<String, Any?>("name" to "Joe", "surname" to "Bloggs", "courtCases" to listOf(courtCases1, courtCases2))
    val resp = convertTemplateToHtml("sample-doc.mustache", data)
    val doc = Jsoup.parse(resp)
    assertThat(doc.select("h1").text()).isEqualTo("Prisoner Name: Joe Bloggs")
    assertThat(doc.select("li").eachText()).containsExactly(
      "Court Name Is: Birmingham Crown Court",
      "Court Name Is: Nottingham Crown Court",
    )
  }

}