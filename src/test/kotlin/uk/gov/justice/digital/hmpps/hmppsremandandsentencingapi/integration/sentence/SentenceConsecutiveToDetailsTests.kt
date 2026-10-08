package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.sentence

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.SentenceConsecutiveToDetailsResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.PeriodLengthEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceEntityStatus

class SentenceConsecutiveToDetailsTests : IntegrationTestBase() {

  @ParameterizedTest
  @ValueSource(
    strings = [
      "ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI",
      "ROLE_REMAND_AND_SENTENCING__CCRD__RO",
      "ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI,ROLE_REMAND_AND_SENTENCING__CCRD__RO",
    ],
  )
  fun `returns sentences to chain to grouped by appearance when there is an active sentence in the past`(roleCsv: String) {
    val roles = roleCsv.split(",")
    val (_, createCourtCase) = createCourtCase()
    val appearance = createCourtCase.appearances.first()
    val charge = appearance.charges.first()
    val sentence = charge.sentence!!
    val result = webTestClient.get()
      .uri {
        it.path("/sentence/consecutive-to-details")
          .queryParam("sentenceUuids", sentence.sentenceUuid)
          .build()
      }
      .headers { it.authToken(roles = roles) }
      .exchange()
      .expectStatus()
      .isOk
      .returnResult(SentenceConsecutiveToDetailsResponse::class.java)
      .responseBody.blockFirst()!!

    Assertions.assertThat(result.sentences).hasSize(1)
    val sentenceConsecutiveToDetails = result.sentences[0]
    Assertions.assertThat(sentenceConsecutiveToDetails.appearanceDate).isEqualTo(appearance.appearanceDate)
    Assertions.assertThat(sentenceConsecutiveToDetails.courtCode).isEqualTo(appearance.courtCode)
    Assertions.assertThat(sentenceConsecutiveToDetails.courtCaseReference).isEqualTo(appearance.courtCaseReference)
    Assertions.assertThat(sentenceConsecutiveToDetails.sentenceUuid).isEqualTo(sentence.sentenceUuid!!)
    Assertions.assertThat(sentenceConsecutiveToDetails.offenceCode).isEqualTo(charge.offenceCode)
    Assertions.assertThat(sentenceConsecutiveToDetails.offenceStartDate).isEqualTo(charge.offenceStartDate)
    Assertions.assertThat(sentenceConsecutiveToDetails.offenceEndDate).isEqualTo(charge.offenceEndDate)
    Assertions.assertThat(sentenceConsecutiveToDetails.countNumber).isEqualTo(sentence.chargeNumber)
    Assertions.assertThat(sentenceConsecutiveToDetails.status).isEqualTo(SentenceEntityStatus.ACTIVE)
  }

  @Test
  fun `no token results in unauthorized`() {
    val (_, createCourtCase) = createCourtCase()
    val appearance = createCourtCase.appearances.first()
    val sentence = appearance.charges.first().sentence!!
    webTestClient.get()
      .uri {
        it.path("/sentence/consecutive-to-details")
          .queryParam("sentenceUuids", sentence.sentenceUuid)
          .build()
      }
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `token with incorrect role is forbidden`() {
    val (_, createCourtCase) = createCourtCase()
    val appearance = createCourtCase.appearances.first()
    val sentence = appearance.charges.first().sentence!!
    webTestClient.get()
      .uri {
        it.path("/sentence/consecutive-to-details")
          .queryParam("sentenceUuids", sentence.sentenceUuid)
          .build()
      }
      .headers { it.authToken(roles = listOf("ROLE_OTHER_FUNCTION")) }
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `do not fix many charges to single sentence when is prisoner read only`() {
    val sentence = DataCreator.migrationCreateSentence()
    val firstCharge = DataCreator.migrationCreateCharge(sentence = sentence)
    val secondCharge = DataCreator.migrationCreateCharge(chargeNOMISId = 1111, sentence = sentence)
    val appearance = DataCreator.migrationCreateCourtAppearance(charges = listOf(firstCharge, secondCharge))
    val courtCase = DataCreator.migrationCreateCourtCase(appearances = listOf(appearance))
    val courtCases = DataCreator.migrationCreateCourtCases(courtCases = listOf(courtCase))

    val response = migrateCases(courtCases)

    val sentenceUuid = response.sentences.first { sentence.sentenceId == it.sentenceNOMISId }.sentenceUuid
    val periodLengthUuid = response.sentenceTerms.first { sentence.periodLengths.first().periodLengthId == it.sentenceTermNOMISId }.periodLengthUuid
    webTestClient.get()
      .uri {
        it.path("/sentence/consecutive-to-details")
          .queryParam("sentenceUuids", sentenceUuid)
          .queryParam("isPrisonerReadOnly", true)
          .build()
      }
      .headers { it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__CCRD__RO")) }
      .exchange()
      .expectStatus()
      .isOk

    val sentenceEntities = sentenceRepository.findBySentenceUuid(sentenceUuid)
    Assertions.assertThat(sentenceEntities).extracting<SentenceEntityStatus> { it.statusId }.allMatch { it == SentenceEntityStatus.MANY_CHARGES_DATA_FIX }
    val periodLengthEntities = periodLengthRepository.findByPeriodLengthUuid(periodLengthUuid)
    Assertions.assertThat(periodLengthEntities).extracting<PeriodLengthEntityStatus> { it.statusId }.allMatch { it == PeriodLengthEntityStatus.MANY_CHARGES_DATA_FIX }
  }
}
