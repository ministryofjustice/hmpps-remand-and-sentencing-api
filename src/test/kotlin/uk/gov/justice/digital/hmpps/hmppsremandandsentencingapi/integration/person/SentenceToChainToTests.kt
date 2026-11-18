package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.person

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.PeriodLength
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.SentencesToChainToResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.PeriodLengthType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import java.math.BigDecimal
import java.time.format.DateTimeFormatter

class SentenceToChainToTests : IntegrationTestBase() {

  @Test
  fun `returns sentences to chain to grouped by appearance when there is an active sentence in the past`() {
    val (_, createCourtCase) = createCourtCase(
      DpsDataCreator.dpsCreateCourtCase(
        appearances = listOf(
          DpsDataCreator.dpsCreateCourtAppearance(
            charges = listOf(
              DpsDataCreator.dpsCreateCharge(chargeValue = BigDecimal("100.50")),
            ),
          ),
        ),
      ),
    )
    val appearance = createCourtCase.appearances.first()
    val charge = appearance.charges.first()
    val sentence = charge.sentence!!
    val bookingId = addBookingIdToDpsSentence(sentence.sentenceUuid!!, charge.chargeUuid, appearance.appearanceUuid)

    val result = webTestClient.get()
      .uri {
        it.path("/person/{prisonerId}/sentences-to-chain-to")
          .queryParam("beforeOrOnAppearanceDate", appearance.appearanceDate.plusDays(5).format(DateTimeFormatter.ISO_DATE))
          .queryParam("bookingId", bookingId.toString())
          .build(createCourtCase.prisonerId)
      }
      .headers { it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI")) }
      .exchange()
      .expectStatus()
      .isOk
      .returnResult(SentencesToChainToResponse::class.java)
      .responseBody.blockFirst()!!

    Assertions.assertThat(result.appearances).hasSize(1)
    val appearanceToChainTo = result.appearances[0]
    Assertions.assertThat(appearanceToChainTo.appearanceDate).isEqualTo(appearance.appearanceDate)
    Assertions.assertThat(appearanceToChainTo.courtCode).isEqualTo(appearance.courtCode)
    Assertions.assertThat(appearanceToChainTo.courtCaseReference).isEqualTo(appearance.courtCaseReference)
    val sentenceToChainTo = appearanceToChainTo.sentences.first()
    Assertions.assertThat(sentenceToChainTo.sentenceUuid).isEqualTo(sentence.sentenceUuid!!)
    Assertions.assertThat(sentenceToChainTo.offenceCode).isEqualTo(charge.offenceCode)
    Assertions.assertThat(sentenceToChainTo.offenceStartDate).isEqualTo(charge.offenceStartDate)
    Assertions.assertThat(sentenceToChainTo.offenceEndDate).isEqualTo(charge.offenceEndDate)
    Assertions.assertThat(sentenceToChainTo.countNumber).isEqualTo(sentence.chargeNumber)
    Assertions.assertThat(sentenceToChainTo.value).isEqualTo(charge.value)
    Assertions.assertThat(sentenceToChainTo.periodLengths).hasSize(sentence.periodLengths.size)
    val periodLength = sentenceToChainTo.periodLengths.first()
    val expectedPeriodLength = sentence.periodLengths.first()
    Assertions.assertThat(periodLength.years).isEqualTo(expectedPeriodLength.years)
    Assertions.assertThat(periodLength.months).isEqualTo(expectedPeriodLength.months)
    Assertions.assertThat(periodLength.weeks).isEqualTo(expectedPeriodLength.weeks)
    Assertions.assertThat(periodLength.days).isEqualTo(expectedPeriodLength.days)
    Assertions.assertThat(periodLength.periodOrder).isEqualTo(expectedPeriodLength.periodOrder)
    Assertions.assertThat(periodLength.periodLengthType).isEqualTo(expectedPeriodLength.type)
  }

  @Test
  fun `returns all period lengths for a sentence with multiple period lengths`() {
    val (_, createCourtCase) = createCourtCase(
      DpsDataCreator.dpsCreateCourtCase(
        appearances = listOf(
          DpsDataCreator.dpsCreateCourtAppearance(
            charges = listOf(
              DpsDataCreator.dpsCreateCharge(
                sentence = DpsDataCreator.dpsCreateSentence(
                  periodLengths = listOf(
                    DpsDataCreator.dpsCreatePeriodLength(
                      years = 2,
                      months = 6,
                      periodOrder = "years,months",
                      type = PeriodLengthType.SENTENCE_LENGTH,
                    ),
                    DpsDataCreator.dpsCreatePeriodLength(
                      years = 1,
                      months = 0,
                      periodOrder = "years,months",
                      type = PeriodLengthType.LICENCE_PERIOD,
                    ),
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    )
    val appearance = createCourtCase.appearances.first()
    val charge = appearance.charges.first()
    val sentence = charge.sentence!!
    val bookingId = addBookingIdToDpsSentence(sentence.sentenceUuid!!, charge.chargeUuid, appearance.appearanceUuid)

    val result = webTestClient.get()
      .uri {
        it.path("/person/{prisonerId}/sentences-to-chain-to")
          .queryParam("beforeOrOnAppearanceDate", appearance.appearanceDate.plusDays(5).format(DateTimeFormatter.ISO_DATE))
          .queryParam("bookingId", bookingId.toString())
          .build(createCourtCase.prisonerId)
      }
      .headers { it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI")) }
      .exchange()
      .expectStatus()
      .isOk
      .returnResult(SentencesToChainToResponse::class.java)
      .responseBody.blockFirst()!!

    Assertions.assertThat(result.appearances).hasSize(1)
    val appearanceToChainTo = result.appearances[0]
    Assertions.assertThat(appearanceToChainTo.sentences).hasSize(1)
    val sentenceToChainTo = appearanceToChainTo.sentences.first()
    Assertions.assertThat(sentenceToChainTo.sentenceUuid).isEqualTo(sentence.sentenceUuid)
    Assertions.assertThat(sentenceToChainTo.periodLengths).containsExactlyInAnyOrderElementsOf(
      sentence.periodLengths.map {
        PeriodLength(
          years = it.years,
          months = it.months,
          weeks = it.weeks,
          days = it.days,
          periodOrder = it.periodOrder,
          periodLengthType = it.type,
          legacyData = it.legacyData,
          periodLengthUuid = it.periodLengthUuid,
        )
      },
    )
  }

  @Test
  fun `returns sentence with empty period lengths when sentence has no period lengths`() {
    val (_, createCourtCase) = createCourtCase(
      DpsDataCreator.dpsCreateCourtCase(
        appearances = listOf(
          DpsDataCreator.dpsCreateCourtAppearance(
            charges = listOf(
              DpsDataCreator.dpsCreateCharge(
                sentence = DpsDataCreator.dpsCreateSentence(
                  periodLengths = emptyList(),
                ),
              ),
            ),
          ),
        ),
      ),
    )
    val appearance = createCourtCase.appearances.first()
    val charge = appearance.charges.first()
    val sentence = charge.sentence!!
    val bookingId = addBookingIdToDpsSentence(sentence.sentenceUuid!!, charge.chargeUuid, appearance.appearanceUuid)

    val result = webTestClient.get()
      .uri {
        it.path("/person/{prisonerId}/sentences-to-chain-to")
          .queryParam("beforeOrOnAppearanceDate", appearance.appearanceDate.plusDays(5).format(DateTimeFormatter.ISO_DATE))
          .queryParam("bookingId", bookingId.toString())
          .build(createCourtCase.prisonerId)
      }
      .headers { it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI")) }
      .exchange()
      .expectStatus()
      .isOk
      .returnResult(SentencesToChainToResponse::class.java)
      .responseBody.blockFirst()!!

    Assertions.assertThat(result.appearances).hasSize(1)
    val appearanceToChainTo = result.appearances[0]
    Assertions.assertThat(appearanceToChainTo.sentences).hasSize(1)
    val sentenceToChainTo = appearanceToChainTo.sentences.first()
    Assertions.assertThat(sentenceToChainTo.sentenceUuid).isEqualTo(sentence.sentenceUuid)
    Assertions.assertThat(sentenceToChainTo.periodLengths).isEmpty()
  }

  @Test
  fun `returns no appearances when there is only an active sentence in the future`() {
    val (_, createCourtCase) = createCourtCase()
    val appearance = createCourtCase.appearances.first()
    val charge = appearance.charges.first()
    val sentence = charge.sentence!!
    val bookingId = addBookingIdToDpsSentence(sentence.sentenceUuid!!, charge.chargeUuid, appearance.appearanceUuid)

    val result = webTestClient.get()
      .uri {
        it.path("/person/{prisonerId}/sentences-to-chain-to")
          .queryParam("beforeOrOnAppearanceDate", appearance.appearanceDate.minusDays(5).format(DateTimeFormatter.ISO_DATE))
          .queryParam("bookingId", bookingId.toString())
          .build(createCourtCase.prisonerId)
      }
      .headers { it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI")) }
      .exchange()
      .expectStatus()
      .isOk
      .returnResult(SentencesToChainToResponse::class.java)
      .responseBody.blockFirst()!!

    Assertions.assertThat(result.appearances).isEmpty()
  }

  @Test
  fun `no token results in unauthorized`() {
    val (_, createCourtCase) = createCourtCase()
    val appearance = createCourtCase.appearances.first()
    val charge = appearance.charges.first()
    val sentence = charge.sentence!!
    val bookingId = addBookingIdToDpsSentence(sentence.sentenceUuid!!, charge.chargeUuid, appearance.appearanceUuid)

    webTestClient.get()
      .uri {
        it.path("/person/{prisonerId}/sentences-to-chain-to")
          .queryParam("beforeOrOnAppearanceDate", appearance.appearanceDate.plusDays(5).format(DateTimeFormatter.ISO_DATE))
          .queryParam("bookingId", bookingId.toString())
          .build(createCourtCase.prisonerId)
      }
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `token with incorrect role is forbidden`() {
    val (_, createCourtCase) = createCourtCase()
    val appearance = createCourtCase.appearances.first()
    val charge = appearance.charges.first()
    val sentence = charge.sentence!!
    val bookingId = addBookingIdToDpsSentence(sentence.sentenceUuid!!, charge.chargeUuid, appearance.appearanceUuid)

    webTestClient.get()
      .uri {
        it.path("/person/{prisonerId}/sentences-to-chain-to")
          .queryParam("beforeOrOnAppearanceDate", appearance.appearanceDate.plusDays(5).format(DateTimeFormatter.ISO_DATE))
          .queryParam("bookingId", bookingId.toString())
          .build(createCourtCase.prisonerId)
      }
      .headers { it.authToken(roles = listOf("ROLE_OTHER_FUNCTION")) }
      .exchange()
      .expectStatus()
      .isForbidden
  }
}
