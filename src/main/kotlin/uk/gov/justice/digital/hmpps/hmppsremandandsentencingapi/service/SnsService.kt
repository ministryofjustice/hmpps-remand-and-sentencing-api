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

  fun <T> publishDomainEvent(
    eventType: String,
    description: String,
    detailsUrl: String,
    timeUpdated: ZonedDateTime,
    additionalInformation: T,
    personReference: PersonReference,
  ) {
    val hmppsMessage = HmppsMessage(eventType, 1, description, detailsUrl, timeUpdated.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME), additionalInformation, personReference)
    domainEventsTopic.publish(hmppsMessage.eventType, objectMapper.writeValueAsString(hmppsMessage), attributes = mapOf(EVENT_TYPE to MessageAttributeValue.builder().dataType(STRING).stringValue(hmppsMessage.eventType).build()))
  }

  private fun generateDetailsUri(path: String, id: String): String = UriComponentsBuilder.newInstance().scheme("https").host(ingressUrl).path(path).buildAndExpand(id).toUriString()
}
