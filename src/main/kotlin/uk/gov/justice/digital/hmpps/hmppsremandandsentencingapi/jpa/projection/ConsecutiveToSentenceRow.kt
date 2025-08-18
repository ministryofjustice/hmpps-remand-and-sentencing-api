package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.projection

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventMetadata
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.RecordEventMetadata
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.ChargeLegacyData
import java.time.LocalDate
import java.util.UUID

data class ConsecutiveToSentenceRow(
  val prisonerId: String,
  val courtCaseId: String,
  val appearanceUuid: UUID,
  val appearanceCourtCode: String,
  val appearanceCourtCaseReference: String?,
  val appearanceDate: LocalDate,
  val chargeUuid: UUID,
  val offenceCode: String,
  val offenceStartDate: LocalDate?,
  val offenceEndDate: LocalDate?,
  val sentenceUuid: UUID,
  val countNumber: String?,
  val chargeLegacyData: ChargeLegacyData?,
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
      chargeUuid.toString(),
      sentenceUuid.toString(),
      null,
      EventType.METADATA_ONLY,
    ),
  )
}
