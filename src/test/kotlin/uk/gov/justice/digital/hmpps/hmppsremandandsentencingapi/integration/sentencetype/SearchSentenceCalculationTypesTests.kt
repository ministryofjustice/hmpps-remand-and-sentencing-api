package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.sentencetype

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.typeReference
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.SentenceType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceTypeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ReferenceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceTypeClassification
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.SentenceTypeRepository
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.stream.Stream

class SearchSentenceCalculationTypesTests : IntegrationTestBase() {

  @Autowired
  private lateinit var sentenceTypeRepository: SentenceTypeRepository

  @Test
  fun `providing no parameters results in bad request`() {
    webTestClient.get()
      .uri("/sentence-type/search")
      .headers { it.authToken() }
      .exchange()
      .expectStatus()
      .isBadRequest
  }

  @Test
  fun `return inactive sentence types`() {
    val convictionDate = LocalDate.now()
    val offenceDate = LocalDate.now()
    val inactiveSentenceType = sentenceTypeRepository.save(
      SentenceTypeEntity(
        sentenceTypeUuid = UUID.randomUUID(),
        description = "Inactive sentence type",
        minAgeInclusive = 1,
        maxAgeExclusive = 99,
        minDateInclusive = convictionDate.minusDays(10),
        maxDateExclusive = convictionDate.plusDays(10),
        classification = SentenceTypeClassification.STANDARD,
        hintText = null,
        nomisCjaCode = "CJA",
        nomisSentenceCalcType = "CalcType",
        displayOrder = 1000,
        status = ReferenceEntityStatus.INACTIVE,
        minOffenceDateInclusive = offenceDate.minusDays(10),
        maxOffenceDateExclusive = offenceDate.plusDays(10),
        isRecallable = true,
      ),
    )
    val result = webTestClient.get()
      .uri(
        "/sentence-type/search?age=18&convictionDate=${convictionDate.format(DateTimeFormatter.ISO_DATE)}&statuses=INACTIVE&offenceDate=${offenceDate.format(
          DateTimeFormatter.ISO_DATE,
        )}",
      )
      .headers { it.authToken() }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody(typeReference<List<SentenceType>>())
      .returnResult().responseBody!!

    Assertions.assertThat(result).extracting<UUID> { it.sentenceTypeUuid }.containsOnly(inactiveSentenceType.sentenceTypeUuid)
    sentenceTypeRepository.delete(inactiveSentenceType)
  }

  @ParameterizedTest(name = "Sentence type bucket test, age {0} on conviction date {1} and offence date {3}")
  @MethodSource("sentenceTypeParameters")
  fun `sentence type bucket tests`(age: Int, convictionDate: LocalDate, expectedDescriptions: List<String>, offenceDate: LocalDate) {
    val result = webTestClient.get()
      .uri(
        "/sentence-type/search?age=$age&convictionDate=${convictionDate.format(DateTimeFormatter.ISO_DATE)}&offenceDate=${offenceDate.format(
          DateTimeFormatter.ISO_DATE,
        )}",
      )
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
    fun sentenceTypeParameters(): Stream<Arguments> = Stream.of(
      Arguments.of(
        25,
        LocalDate.parse("2020-12-15"),
        listOf(
          "Imprisonment in Default of Fine",
          "ORA SDS (Offender rehabilitation act standard determinate sentence)",
          "SDS (Standard Determinate Sentence)",
          "Automatic Life",
          "ORA Breach Top Up Supervision",
          "Civil Imprisonment",
          "Adult Discretionary Life",
          "EDS (Extended Determinate Sentence)",
          "Adult Mandatory Life",
          "SOPC (offenders of a particular concern)",
          "Serious Terrorism Sentence",
        ),
        LocalDate.parse("2020-12-15"),
      ),
      Arguments.of(
        19,
        LocalDate.parse("2020-12-15"),
        listOf(
          "Imprisonment in Default of Fine",
          "Automatic Life Sec 273 Sentencing Code (18 - 20)",
          "ORA Breach Top Up Supervision",
          "Civil Imprisonment",
          "Detention For Life",
          "Adult Discretionary Life",
          "EDS (Extended Determinate Sentence)",
          "Detention During His Majesty's Pleasure",
          "Custody For Life Sec 272 Sentencing Code (18 - 20)",
          "Custody For Life Sec 275 Sentencing Code (Murder) (U21)",
          "SOPC (offenders of a particular concern)",
          "Serious Terrorism Sentence",
          "YOI ORA (Young offender Institution offender rehabilitation act)",
        ),
        LocalDate.parse("2020-12-15"),
      ),
      Arguments.of(
        17,
        LocalDate.parse("2020-12-15"),
        listOf(
          "Imprisonment in Default of Fine",
          "Detention For Life",
          "ORA Detention and Training Order",
          "EDS (Extended Determinate Sentence)",
          "Detention During His Majesty's Pleasure",
          "SDOPC (Special sentence of detention for terrorist offenders of particular concern)",
          "ORA Serious Offence Sec 250 Sentencing Code (U18)",
          "Custody For Life Sec 275 Sentencing Code (Murder) (U21)",
        ),
        LocalDate.parse("2020-12-15"),
      ),
      Arguments.of(
        20,
        LocalDate.parse("2020-11-15"),
        listOf(
          "ORA SDS (Offender rehabilitation act standard determinate sentence)",
          "SDS (Standard Determinate Sentence)",
          "Automatic Life",
          "Automatic Life Sec 224A 03",
          "ORA Breach Top Up Supervision",
          "Civil Imprisonment",
          "Detention For Life",
          "Adult Discretionary Life",
          "Extended Sentence for Public Protection",
          "Detention During His Majesty's Pleasure",
          "Indeterminate Sentence for the Public Protection",
          "EDS LASPO Automatic Release",
          "EDS LASPO Discretionary Release",
          "Adult Mandatory Life",
          "Section 236A SOPC CJA03",
          "Custody For Life - Under 21 CJA03",
          "Custody Life (18-21 Years Old)",
          "Legacy (1967 Act)",
          "Legacy (1991 Act)",
        ),
        LocalDate.parse("2020-12-15"),
      ),
      Arguments.of(
        17,
        LocalDate.parse("2020-11-15"),
        listOf(
          "Detention For Life",
          "Detention For Public Protection",
          "ORA Detention and Training Order",
          "Extended Sentence for Public Protection",
          "Detention During His Majesty's Pleasure",
          "Serious Offence -18 CJA03 POCCA 2000",
          "ORA (Offender rehabilitation act)",
          "Custody For Life - Under 21 CJA03",
          "YOI ORA (Young offender Institution offender rehabilitation act)",
          "Legacy (1967 Act)",
          "Legacy (1991 Act)",
        ),
        LocalDate.parse("2020-12-15"),
      ),
    )
  }
}
