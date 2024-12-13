package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.HmppsMessage
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.PersonReference
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
) {
  private val domainEventsTopic by lazy {
    hmppsQueueService.findByTopicId("hmppsdomaintopic")
      ?: throw RuntimeException("Topic with name hmppsdomaintopic doesn't exist")
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
}
