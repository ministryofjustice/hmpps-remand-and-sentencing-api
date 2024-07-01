package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.util.UriComponentsBuilder
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.HmppsCourtCaseMessage
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.HmppsMessage
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.PersonReference
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.PersonReferenceType
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

private const val EVENT_TYPE = "eventType"

private const val STRING = "String"

@Service
class SnsService(
  hmppsQueueService: HmppsQueueService,
  private val objectMapper: ObjectMapper,
  @Value("\${ingress.url}") private val ingressUrl: String,
  @Value("\${court.case.getByIdPath}") private val courtCaseLookupPath: String
) {
  private val domainEventsTopic by lazy {
    hmppsQueueService.findByTopicId("hmppsdomaintopic")
      ?: throw RuntimeException("Topic with name hmppsdomaintopic doesn't exist")
  }
  private val domainEventsTopicClient by lazy { domainEventsTopic.snsClient }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
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
    domainEventsTopicClient.publish(
      PublishRequest.builder()
        .topicArn(domainEventsTopic.arn)
        .message(objectMapper.writeValueAsString(hmppsCourtCaseInsertedEvent))
        .messageAttributes(
          mapOf(EVENT_TYPE to MessageAttributeValue.builder().dataType(STRING).stringValue(hmppsCourtCaseInsertedEvent.eventType).build()),
        ).build(),
    ).get()
  }

  private fun generateDetailsUri(path: String, id: String): String = UriComponentsBuilder.newInstance().scheme("https").host(ingressUrl).path(path).buildAndExpand(id).toUriString()

}
