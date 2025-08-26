package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.booking

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.BookingDataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.booking.BookingCreateCourtCasesResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.service.LegacySentenceService

class BookingConsecutiveToTests : IntegrationTestBase() {

  @Test
  fun `can create sentence when consecutive to another in the same court case`() {
    val firstSentenceId = BookingDataCreator.bookingSentenceId(1, 1)
    val firstPeriodLengthId = DataCreator.nomisPeriodLengthId(firstSentenceId.offenderBookingId, firstSentenceId.sequence, 1)
    val firstSentence = BookingDataCreator.bookingCreateSentence(
      sentenceId = firstSentenceId,
      legacyData = DataCreator.sentenceLegacyData(sentenceCalcType = "FTR_ORA"),
      periodLengths = listOf(
        BookingDataCreator.bookingCreatePeriodLength(periodLengthId = firstPeriodLengthId),
      ),
    )

    val consecutiveToSentenceId = BookingDataCreator.bookingSentenceId(1, 5)
    val consecutiveToPeriodLengthId = DataCreator.nomisPeriodLengthId(consecutiveToSentenceId.offenderBookingId, consecutiveToSentenceId.sequence, 1)
    val consecutiveToSentence = BookingDataCreator.bookingCreateSentence(
      sentenceId = consecutiveToSentenceId,
      consecutiveToSentenceId = firstSentence.sentenceId,
      periodLengths = listOf(
        BookingDataCreator.bookingCreatePeriodLength(periodLengthId = consecutiveToPeriodLengthId),
      ),
    )
    val charge = BookingDataCreator.bookingCreateCharge(chargeNOMISId = 11, sentence = firstSentence)
    val consecutiveToCharge = BookingDataCreator.bookingCreateCharge(chargeNOMISId = 22, sentence = consecutiveToSentence)
    val appearance = BookingDataCreator.bookingCreateCourtAppearance(charges = listOf(consecutiveToCharge, charge))
    val bookingCourtCase = BookingDataCreator.bookingCreateCourtCase(appearances = listOf(appearance))
    val bookingCourtCases = BookingDataCreator.bookingCreateCourtCases(courtCases = listOf(bookingCourtCase))
    val response = webTestClient
      .post()
      .uri("/legacy/court-case/booking")
      .bodyValue(bookingCourtCases)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_COURT_CASE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated
      .returnResult(BookingCreateCourtCasesResponse::class.java)
      .responseBody.blockFirst()!!

    val consecutiveToSentenceUuid = response.sentences.first { sentenceResponse -> sentenceResponse.sentenceNOMISId == consecutiveToSentence.sentenceId }.sentenceUuid
    val firstSentenceUuid = response.sentences.first { sentenceResponse -> sentenceResponse.sentenceNOMISId == firstSentence.sentenceId }.sentenceUuid
    val consecutiveToSentenceEntity = sentenceRepository.findBySentenceUuid(consecutiveToSentenceUuid).first()
    val firstSentenceEntity = sentenceRepository.findBySentenceUuid(firstSentenceUuid).first()
    Assertions.assertThat(consecutiveToSentenceEntity.consecutiveTo!!.sentenceUuid).isEqualTo(firstSentenceUuid)
    Assertions.assertThat(consecutiveToSentenceEntity.sentenceServeType).isEqualTo("CONSECUTIVE")
    Assertions.assertThat(firstSentenceEntity.sentenceType!!.sentenceTypeUuid).isEqualTo(LegacySentenceService.recallSentenceTypeBucketUuid)
  }

  @Test
  fun `can still process a sentence where the consecutive to sentence is non existent (A NOMIS data issue)`() {
    val sentenceWithNonExistentConsecutiveTo = BookingDataCreator.bookingCreateSentence(sentenceId = BookingDataCreator.bookingSentenceId(1, 1), consecutiveToSentenceId = BookingDataCreator.bookingSentenceId(66, 99), legacyData = DataCreator.sentenceLegacyData(sentenceCalcType = "FTR_ORA"))
    val charge = BookingDataCreator.bookingCreateCharge(chargeNOMISId = 11, sentence = sentenceWithNonExistentConsecutiveTo)
    val appearance = BookingDataCreator.bookingCreateCourtAppearance(charges = listOf(charge))
    val bookingCourtCase = BookingDataCreator.bookingCreateCourtCase(appearances = listOf(appearance))
    val bookingCourtCases = BookingDataCreator.bookingCreateCourtCases(courtCases = listOf(bookingCourtCase))
    val response = webTestClient
      .post()
      .uri("/legacy/court-case/booking")
      .bodyValue(bookingCourtCases)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_COURT_CASE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated
      .returnResult(BookingCreateCourtCasesResponse::class.java)
      .responseBody.blockFirst()!!
    val sentenceUuid = response.sentences.first { sentenceResponse -> sentenceResponse.sentenceNOMISId == sentenceWithNonExistentConsecutiveTo.sentenceId }.sentenceUuid
    val savedSentenceEntity = sentenceRepository.findBySentenceUuid(sentenceUuid).first()
    Assertions.assertThat(savedSentenceEntity.sentenceType!!.sentenceTypeUuid).isEqualTo(LegacySentenceService.recallSentenceTypeBucketUuid)
  }
}
