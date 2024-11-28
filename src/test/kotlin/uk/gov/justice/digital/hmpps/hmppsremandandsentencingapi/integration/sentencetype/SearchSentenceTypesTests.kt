package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.sentencetype

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.core.ParameterizedTypeReference
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.SentenceType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.stream.Stream

class SearchSentenceTypesTests : IntegrationTestBase() {

  private inline fun <reified T> typeReference() = object : ParameterizedTypeReference<T>() {}

  @Test
  fun `providing no parameters results in bad request`() {
    webTestClient.get()
      .uri("/sentence-type/search")
      .headers { it.authToken() }
      .exchange()
      .expectStatus()
      .isBadRequest
  }

  @ParameterizedTest(name = "Sentence type bucket test, age {0} on date {1}")
  @MethodSource("sentenceTypeParameters")
  fun `sentence type bucket tests`(age: Int, convictionDate: LocalDate, expectedDescriptions: List<String>) {
    val result = webTestClient.get()
      .uri("/sentence-type/search?age=$age&convictionDate=${convictionDate.format(DateTimeFormatter.ISO_DATE)}")
      .headers { it.authToken() }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody(typeReference<List<SentenceType>>())
      .returnResult().responseBody!!
    val descriptions = result.map { it.description }
    Assertions.assertThat(descriptions).containsExactlyInAnyOrderElementsOf(expectedDescriptions)
  }

  companion object {
    @JvmStatic
    fun sentenceTypeParameters(): Stream<Arguments> {
      return Stream.of(
        Arguments.of(
          25,
          LocalDate.parse("2020-12-15"),
          listOf(
            "Imprisonment in default of fine",
            "SDS (Standard determinate sentence)",
            "ORA SDS (Offender rehabilitation act standard determinate sentence)",
            "Automatic life",
            "ORA Breach top up supervision",
            "Civil imprisonment",
            "Adult discretionary life",
            "EDS (Extended determinate sentence)",
            "Adult mandatory life",
            "SOPC (Offenders of a particular concern)",
            "Serious terrorism sentence",
            "Violent offender order",
          ),
        ),
        Arguments.of(
          19,
          LocalDate.parse("2020-12-15"),
          listOf(
            "Imprisonment in default of fine",
            "Automatic life sec 273 sentencing code (18 - 20)",
            "ORA Breach top up supervision",
            "Civil imprisonment",
            "Detention for life",
            "Adult discretionary life",
            "EDS (Extended determinate sentence)",
            "Detention during his majesty's pleasure",
            "Custody for life sec 272 sentencing code (18 - 20)",
            "Custody for life sec 275 sentencing code (murder) (U21)",
            "SOPC (Offenders of a particular concern)",
            "Serious terrorism sentence",
            "Violent offender order",
            "YOI (Young offender institution)",
            "YOI ORA (Young offender institution offender rehabilitation act)",
          ),
        ),
        Arguments.of(
          17,
          LocalDate.parse("2020-12-15"),
          listOf(
            "Imprisonment in default of fine",
            "Detention For life",
            "Detention and training order",
            "ORA Detention and training order",
            "EDS (Extended determinate sentence)",
            "Detention during his majesty's pleasure",
            "SDOPC (Special sentence of detention for terrorist offenders of particular concern)",
            "SDS (Standard determinate sentence)",
            "ORA (Offender rehabilitation act)",
            "Custody for life sec 275 sentencing code (murder) (U21)",
            "Youth rehabilitation order",
          ),
        ),
        Arguments.of(
          20,
          LocalDate.parse("2020-11-15"),
          listOf(
            "SDS (Standard determinate sentence)",
            "ORA SDS (Offender rehabilitation act standard determinate sentence)",
            "Automatic life",
            "Automatic life Sec 224A 03",
            "ORA Breach top up supervision",
            "Civil imprisonment",
            "Detention for life",
            "Adult discretionary life",
            "Extended sentence for public protection",
            "Detention during his majesty's pleasure",
            "Indeterminate sentence for the public protection",
            "EDS LASPO Automatic release",
            "EDS LASPO Discretionary release",
            "Adult mandatory life",
            "Section 236A SOPC CJA03",
            "Custody for life - under 21 CJA03",
            "Custody life (18-21 years old)",
            "Legacy (1967 act)",
            "Legacy (1991 act)",
          ),
        ),
        Arguments.of(
          17,
          LocalDate.parse("2020-11-15"),
          listOf(
            "Detention for life",
            "Detention for public protection",
            "Detention and training order",
            "ORA Detention and training order",
            "Extended sentence for public protection",
            "Detention during his majesty's pleasure",
            "Serious offence -18 CJA03 POCCA 2000",
            "ORA (Offender rehabilitation act)",
            "Custody for life - under 21 CJA03",
            "YOI (Young offender institution)",
            "YOI ORA (Young offender institution offender rehabilitation act)",
            "Legacy (1967 act)",
            "Legacy (1991 act)",
            "Section 104",
            "Section 105",
          ),
        ),
      )
    }
  }
}
