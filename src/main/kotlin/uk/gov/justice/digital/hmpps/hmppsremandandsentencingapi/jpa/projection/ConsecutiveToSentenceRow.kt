package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.projection

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventMetadata
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.RecordEventMetadata
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.PeriodLengthType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.ChargeLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.PeriodLengthLegacyData
import java.math.BigDecimal
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
  val status: SentenceEntityStatus,
  val value: BigDecimal? = null,
  val periodLengthUuid: UUID? = null,
  val years: Int? = null,
  val months: Int? = null,
  val weeks: Int? = null,
  val days: Int? = null,
  val periodOrder: String? = null,
  val periodLengthType: PeriodLengthType? = null,
  val periodLengthLegacyData: PeriodLengthLegacyData? = null,
) {
  constructor(
    prisonerId: String,
    courtCaseId: String,
    appearanceUuid: UUID,
    appearanceCourtCode: String,
    appearanceCourtCaseReference: String?,
    appearanceDate: LocalDate,
    chargeUuid: UUID,
    offenceCode: String,
    offenceStartDate: LocalDate?,
    offenceEndDate: LocalDate?,
    sentenceUuid: UUID,
    countNumber: String?,
    chargeLegacyData: ChargeLegacyData?,
    status: SentenceEntityStatus,
    value: BigDecimal?,
  ) : this(
    prisonerId,
    courtCaseId,
    appearanceUuid,
    appearanceCourtCode,
    appearanceCourtCaseReference,
    appearanceDate,
    chargeUuid,
    offenceCode,
    offenceStartDate,
    offenceEndDate,
    sentenceUuid,
    countNumber,
    chargeLegacyData,
    status,
    value,
    null,
    null,
    null,
    null,
    null,
    null,
    null,
    null,
  )

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
