package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.EventSource
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.EventSource.DPS
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
