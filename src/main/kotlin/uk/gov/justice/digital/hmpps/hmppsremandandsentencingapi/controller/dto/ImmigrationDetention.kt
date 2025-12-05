package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.EventSource
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.EventSource.DPS
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.AppearanceChargeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ImmigrationDetentionEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ImmigrationDetentionNoLongerOfInterestType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ImmigrationDetentionRecordType
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

data class ImmigrationDetention(
  val immigrationDetentionUuid: UUID,
  val prisonerId: String,
  var immigrationDetentionRecordType: ImmigrationDetentionRecordType,
  var recordDate: LocalDate,
  var homeOfficeReferenceNumber: String? = null,
  var noLongerOfInterestReason: ImmigrationDetentionNoLongerOfInterestType? = null,
  var noLongerOfInterestComment: String? = null,
  var createdAt: ZonedDateTime,
  var source: EventSource = DPS,
) {
  companion object {
    fun fromCourtAppearance(courtAppearance: CourtAppearanceEntity, prisonerId: String): ImmigrationDetention {
      val chargeOutcomeId = courtAppearance.appearanceCharges
        .map(AppearanceChargeEntity::charge)
        .firstOrNull()?.chargeOutcome?.nomisCode
      return ImmigrationDetention(
        immigrationDetentionUuid = courtAppearance.appearanceUuid,
        prisonerId = prisonerId,
        immigrationDetentionRecordType = getImmigrationDetentionTypeFromNOMIS(chargeOutcomeId),
        recordDate = courtAppearance.appearanceDate,
        homeOfficeReferenceNumber = courtAppearance.courtCaseReference,
        noLongerOfInterestReason = null,
        noLongerOfInterestComment = null,
        createdAt = courtAppearance.createdAt,
        source = courtAppearance.source,
      )
    }

    private fun getImmigrationDetentionTypeFromNOMIS(code: String?): ImmigrationDetentionRecordType = when (code) {
      "5500" -> ImmigrationDetentionRecordType.IS91
      "5502" -> ImmigrationDetentionRecordType.DEPORTATION_ORDER
      else -> ImmigrationDetentionRecordType.UNKNOWN
    }

    fun from(immigrationDetentionEntity: ImmigrationDetentionEntity): ImmigrationDetention = ImmigrationDetention(
      immigrationDetentionUuid = immigrationDetentionEntity.immigrationDetentionUuid,
      prisonerId = immigrationDetentionEntity.prisonerId,
      immigrationDetentionRecordType = immigrationDetentionEntity.immigrationDetentionRecordType,
      recordDate = immigrationDetentionEntity.recordDate,
      homeOfficeReferenceNumber = immigrationDetentionEntity.homeOfficeReferenceNumber,
      noLongerOfInterestReason = immigrationDetentionEntity.noLongerOfInterestReason,
      noLongerOfInterestComment = immigrationDetentionEntity.noLongerOfInterestComment,
      createdAt = immigrationDetentionEntity.createdAt,
      source = immigrationDetentionEntity.source,
    )
  }
}
