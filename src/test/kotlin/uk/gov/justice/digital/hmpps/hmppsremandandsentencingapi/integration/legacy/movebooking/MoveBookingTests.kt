package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.movebooking

import org.assertj.core.api.Assertions
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateRecall
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator.Factory.sentenceLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.RecallType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator.Factory.DEFAULT_PRISONER_ID
import java.time.LocalDate

class MoveBookingTests : IntegrationTestBase() {

  @Test
  fun `move booking from old prisoner id to new prisoner id`() {
    val oldPrisonerId = "OLDPRISONER"
    val bookingId = 1L
    val (courtCaseUuid) = createLegacyCourtCase(DataCreator.legacyCreateCourtCase(prisonerId = oldPrisonerId, legacyData = DataCreator.courtCaseLegacyData(bookingId = bookingId)))
    val newPrisonerId = "NEWPRISONER"
    val eventType = "prison-offender-events.prisoner.booking.moved"
    val payload = prisonerBookingMovedPayload(eventType, bookingId.toString(), oldPrisonerId, newPrisonerId)
    hmppsTopicSnsClient.publish(
      PublishRequest.builder().topicArn(hmppsTopicArn)
        .message(payload)
        .messageAttributes(
          mapOf(
            "eventType" to MessageAttributeValue.builder().dataType("String")
              .stringValue(eventType).build(),
          ),
        ).build(),
    ).get()

    awaitUntilPrisonerQueueIsEmptyAndNoDlq()
    await untilAsserted {
      val storedCourtCase = courtCaseRepository.findByCaseUniqueIdentifier(courtCaseUuid)!!
      Assertions.assertThat(storedCourtCase.prisonerId).isEqualTo(newPrisonerId)
    }
  }

  @Test
  fun `move booking updates recall prisoner id`() {
    val bookingId = 9988771L
    val (sentenceUuid) = createLegacySentence(
      legacySentence = DataCreator.legacyCreateSentence(sentenceLegacyData = sentenceLegacyData(bookingId = bookingId)),
    )

    val (recallUuid) = createRecall(
      CreateRecall(
        revocationDate = LocalDate.now(),
        prisonerId = DEFAULT_PRISONER_ID,
        recallTypeCode = RecallType.FTR_14,
        createdByUsername = "integration-test",
        createdByPrison = "MDI",
        sentenceIds = listOf(sentenceUuid),
      ),
    )

    val newPrisonerId = "NEW_RECALL_PRISONER"
    val eventType = "prison-offender-events.prisoner.booking.moved"
    val payload = prisonerBookingMovedPayload(
      eventType = eventType,
      bookingId = bookingId.toString(),
      oldPersonId = DEFAULT_PRISONER_ID,
      newPersonId = newPrisonerId,
    )
    hmppsTopicSnsClient.publish(
      PublishRequest.builder().topicArn(hmppsTopicArn)
        .message(payload)
        .messageAttributes(
          mapOf(
            "eventType" to MessageAttributeValue.builder().dataType("String")
              .stringValue(eventType).build(),
          ),
        ).build(),
    ).get()

    awaitUntilPrisonerQueueIsEmptyAndNoDlq()
    await untilAsserted {
      val recall = recallRepository.findOneByRecallUuid(recallUuid)
      Assertions.assertThat(recall?.prisonerId).isEqualTo(newPrisonerId)
    }
  }

  private fun prisonerBookingMovedPayload(eventType: String, bookingId: String, oldPersonId: String, newPersonId: String) =
    """
        {
            "eventType": "$eventType",
            "additionalInformation": {
                "bookingId": "$bookingId",
                "movedFromNomsNumber": "$oldPersonId",
                "movedToNomsNumber": "$newPersonId",
                "bookingStartDateTime": "2023-10-01T12:00:00Z"
            },
            "occurredAt": "2023-10-01T12:00:00Z",
            "personReference": {
                "identifiers": [
                    {
                        "type": "NOMS",
                        "value": "$newPersonId"
                    }
                ]
            },
            "publishedAt": "2023-10-01T12:00:00Z",
            "version": 1
        }
    """
}
