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
    domainEventsTopic.publish(hmppsCourtCaseInsertedEvent.eventType, objectMapper.writeValueAsString(hmppsCourtCaseInsertedEvent), mapOf(EVENT_TYPE to MessageAttributeValue.builder().dataType(STRING).stringValue(hmppsCourtCaseInsertedEvent.eventType).build()))
  }

  fun courtAppearanceInserted(prisonerId: String, courtAppearanceId: String, timeUpdated: ZonedDateTime) {
    val hmppsCourtAppearanceInsertedEvent = HmppsMessage(
      "court-appearance.inserted",
      1,
      "Court appearance inserted event",
      generateDetailsUri(courtAppearanceLookupPath, courtAppearanceId),
      timeUpdated.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
      HmppsCourtAppearanceMessage(courtAppearanceId),
      PersonReference(listOf(PersonReferenceType("NOMS", prisonerId))),
    )
    domainEventsTopic.publish(hmppsCourtAppearanceInsertedEvent.eventType, objectMapper.writeValueAsString(hmppsCourtAppearanceInsertedEvent), mapOf(EVENT_TYPE to MessageAttributeValue.builder().dataType(STRING).stringValue(hmppsCourtAppearanceInsertedEvent.eventType).build()))
  }

  fun chargeInserted(prisonerId: String, chargeId: String, timeUpdated: ZonedDateTime) {
    val hmppsCourtAppearanceInsertedEvent = HmppsMessage(
      "charge.inserted",
      1,
      "Charge inserted event",
      generateDetailsUri(courtAppearanceLookupPath, chargeId),
      timeUpdated.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
      HmppsCourtChargeMessage(chargeId),
      PersonReference(listOf(PersonReferenceType("NOMS", prisonerId))),
    )
    domainEventsTopic.publish(hmppsCourtAppearanceInsertedEvent.eventType, objectMapper.writeValueAsString(hmppsCourtAppearanceInsertedEvent), mapOf(EVENT_TYPE to MessageAttributeValue.builder().dataType(STRING).stringValue(hmppsCourtAppearanceInsertedEvent.eventType).build()))
  }

  private fun generateDetailsUri(path: String, id: String): String = UriComponentsBuilder.newInstance().scheme("https").host(ingressUrl).path(path).buildAndExpand(id).toUriString()
}
