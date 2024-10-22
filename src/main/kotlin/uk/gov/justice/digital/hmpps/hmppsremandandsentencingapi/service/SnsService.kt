package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.util.UriComponentsBuilder
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.HmppsCourtAppearanceMessage
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.HmppsCourtCaseMessage
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.HmppsCourtChargeMessage
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.HmppsMessage
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.HmppsSentenceMessage
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.PersonReference
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.PersonReferenceType
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.publish
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

private const val EVENT_TYPE = "eventType"

private const val STRING = "String"

@Service
class SnsService(
  hmppsQueueService: HmppsQueueService,
  private val objectMapper: ObjectMapper,
  @Value("\${ingress.url}") private val ingressUrl: String,
  @Value("\${court.case.getByIdPath}") private val courtCaseLookupPath: String,
  @Value("\${court.appearance.getByIdPath}") private val courtAppearanceLookupPath: String,
  @Value("\${court.charge.getByIdPath}") private val courtChargeLookupPath: String,
  @Value("\${court.sentence.getByIdPath}") private val sentenceLookupPath: String,
) {
  private val domainEventsTopic by lazy {
    hmppsQueueService.findByTopicId("hmppsdomaintopic")
      ?: throw RuntimeException("Topic with name hmppsdomaintopic doesn't exist")
  }

  fun courtCaseInserted(prisonerId: String, courtCaseId: String, timeUpdated: ZonedDateTime) {
    val hmppsCourtCaseInsertedEvent = HmppsMessage(
      "court-case.inserted",
      1,
      "Court case inserted event",
      generateDetailsUri(courtCaseLookupPath, courtCaseId),
      timeUpdated.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
      HmppsCourtCaseMessage(courtCaseId),
      PersonReference(listOf(PersonReferenceType("NOMS", prisonerId))),
    )
    domainEventsTopic.publish(hmppsCourtCaseInsertedEvent.eventType, objectMapper.writeValueAsString(hmppsCourtCaseInsertedEvent), attributes = mapOf(EVENT_TYPE to MessageAttributeValue.builder().dataType(STRING).stringValue(hmppsCourtCaseInsertedEvent.eventType).build()))
  }

  fun courtAppearanceInserted(prisonerId: String, courtAppearanceId: String, courtCaseId: String, timeUpdated: ZonedDateTime) {
    val hmppsCourtAppearanceInsertedEvent = HmppsMessage(
      "court-appearance.inserted",
      1,
      "Court appearance inserted event",
      generateDetailsUri(courtAppearanceLookupPath, courtAppearanceId),
      timeUpdated.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
      HmppsCourtAppearanceMessage(courtAppearanceId, courtCaseId),
      PersonReference(listOf(PersonReferenceType("NOMS", prisonerId))),
    )
    domainEventsTopic.publish(hmppsCourtAppearanceInsertedEvent.eventType, objectMapper.writeValueAsString(hmppsCourtAppearanceInsertedEvent), attributes = mapOf(EVENT_TYPE to MessageAttributeValue.builder().dataType(STRING).stringValue(hmppsCourtAppearanceInsertedEvent.eventType).build()))
  }

  fun courtAppearanceUpdated(prisonerId: String, courtAppearanceId: String, courtCaseId: String, timeUpdated: ZonedDateTime) {
    val hmppsCourtAppearanceUpdatedEvent = HmppsMessage(
      "court-appearance.updated",
      1,
      "Court appearance updated event",
      generateDetailsUri(courtAppearanceLookupPath, courtAppearanceId),
      timeUpdated.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
      HmppsCourtAppearanceMessage(courtAppearanceId, courtCaseId),
      PersonReference(listOf(PersonReferenceType("NOMS", prisonerId))),
    )
    domainEventsTopic.publish(hmppsCourtAppearanceUpdatedEvent.eventType, objectMapper.writeValueAsString(hmppsCourtAppearanceUpdatedEvent), attributes = mapOf(EVENT_TYPE to MessageAttributeValue.builder().dataType(STRING).stringValue(hmppsCourtAppearanceUpdatedEvent.eventType).build()))
  }
  fun courtAppearanceDeleted(prisonerId: String, courtAppearanceId: String, courtCaseId: String, timeUpdated: ZonedDateTime) {
    val hmppsCourtAppearanceDeletedEvent = HmppsMessage(
      "court-appearance.deleted",
      1,
      "Court appearance deleted event",
      generateDetailsUri(courtAppearanceLookupPath, courtAppearanceId),
      timeUpdated.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
      HmppsCourtAppearanceMessage(courtAppearanceId, courtCaseId),
      PersonReference(listOf(PersonReferenceType("NOMS", prisonerId))),
    )
    domainEventsTopic.publish(hmppsCourtAppearanceDeletedEvent.eventType, objectMapper.writeValueAsString(hmppsCourtAppearanceDeletedEvent), attributes = mapOf(EVENT_TYPE to MessageAttributeValue.builder().dataType(STRING).stringValue(hmppsCourtAppearanceDeletedEvent.eventType).build()))
  }

  fun chargeInserted(prisonerId: String, chargeId: String, timeUpdated: ZonedDateTime) {
    val hmppsCourtChargeInsertedEvent = HmppsMessage(
      "charge.inserted",
      1,
      "Charge inserted event",
      generateDetailsUri(courtChargeLookupPath, chargeId),
      timeUpdated.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
      HmppsCourtChargeMessage(chargeId),
      PersonReference(listOf(PersonReferenceType("NOMS", prisonerId))),
    )
    domainEventsTopic.publish(hmppsCourtChargeInsertedEvent.eventType, objectMapper.writeValueAsString(hmppsCourtChargeInsertedEvent), attributes = mapOf(EVENT_TYPE to MessageAttributeValue.builder().dataType(STRING).stringValue(hmppsCourtChargeInsertedEvent.eventType).build()))
  }

  fun chargeUpdated(prisonerId: String, chargeId: String, timeUpdated: ZonedDateTime) {
    val hmppsCourtChargeUpdatedEvent = HmppsMessage(
      "charge.updated",
      1,
      "Charge updated event",
      generateDetailsUri(courtChargeLookupPath, chargeId),
      timeUpdated.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
      HmppsCourtChargeMessage(chargeId),
      PersonReference(listOf(PersonReferenceType("NOMS", prisonerId))),
    )
    domainEventsTopic.publish(hmppsCourtChargeUpdatedEvent.eventType, objectMapper.writeValueAsString(hmppsCourtChargeUpdatedEvent), attributes = mapOf(EVENT_TYPE to MessageAttributeValue.builder().dataType(STRING).stringValue(hmppsCourtChargeUpdatedEvent.eventType).build()))
  }

  fun sentenceInserted(prisonerId: String, sentenceId: String, timeUpdated: ZonedDateTime) {
    val hmppsSentenceInsertedEvent = HmppsMessage(
      "sentence.inserted",
      1,
      "Sentence inserted event",
      generateDetailsUri(sentenceLookupPath, sentenceId),
      timeUpdated.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
      HmppsSentenceMessage(sentenceId),
      PersonReference(listOf(PersonReferenceType("NOMS", prisonerId))),
    )
    domainEventsTopic.publish(hmppsSentenceInsertedEvent.eventType, objectMapper.writeValueAsString(hmppsSentenceInsertedEvent), attributes = mapOf(EVENT_TYPE to MessageAttributeValue.builder().dataType(STRING).stringValue(hmppsSentenceInsertedEvent.eventType).build()))
  }

  fun legacyCaseReferencesUpdated(prisonerId: String, courtCaseId: String, timeUpdated: ZonedDateTime) {
    val hmppsCourtCaseInsertedEvent = HmppsMessage(
      "legacy.court-case-references.updated",
      1,
      "Legacy court case references updated",
      generateDetailsUri(courtCaseLookupPath, courtCaseId),
      timeUpdated.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
      HmppsCourtCaseMessage(courtCaseId),
      PersonReference(listOf(PersonReferenceType("NOMS", prisonerId))),
    )
    domainEventsTopic.publish(hmppsCourtCaseInsertedEvent.eventType, objectMapper.writeValueAsString(hmppsCourtCaseInsertedEvent), attributes = mapOf(EVENT_TYPE to MessageAttributeValue.builder().dataType(STRING).stringValue(hmppsCourtCaseInsertedEvent.eventType).build()))
  }

  private fun generateDetailsUri(path: String, id: String): String = UriComponentsBuilder.newInstance().scheme("https").host(ingressUrl).path(path).buildAndExpand(id).toUriString()
}
