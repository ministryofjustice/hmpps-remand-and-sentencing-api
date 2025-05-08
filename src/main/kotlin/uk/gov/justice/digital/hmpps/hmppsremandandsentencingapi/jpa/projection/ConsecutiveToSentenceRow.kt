package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.projection

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventMetadata
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.RecordEventMetadata
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceEntity
import java.time.LocalDate
import java.util.UUID

data class ConsecutiveToSentenceRow(
  val prisonerId: String,
  val courtCaseId: String,
  val appearanceUuid: UUID,
  val appearanceCourtCode: String,
  val appearanceCourtCaseReference: String?,
  val appearanceDate: LocalDate,
  val charge: ChargeEntity,
  val sentence: SentenceEntity,
) {

  fun toConsecutiveToSentenceAppearance(): ConsecutiveToSentenceAppearance = ConsecutiveToSentenceAppearance(
    appearanceUuid,
    appearanceCourtCode,
    appearanceCourtCaseReference,
    appearanceDate,
  )
  fun <T> toRecordEventMetadata(record: T): RecordEventMetadata<T> = RecordEventMetadata(
    record,
    EventMetadata(
      prisonerId,
      courtCaseId,
      appearanceUuid.toString(),
      charge.chargeUuid.toString(),
      sentence.sentenceUuid.toString(),
      null,
      EventType.METADATA_ONLY,
    ),
  )
}
