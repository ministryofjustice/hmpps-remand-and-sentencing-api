package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.EventSource
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.EventSource.DPS
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ImmigrationDetentionEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ImmigrationDetentionEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ImmigrationDetentionNoLongerOfInterestType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ImmigrationDetentionRecordType
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

@Entity
@Table(name = "immigration_detention_history")
class ImmigrationDetentionHistoryEntity(
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Int = 0,
  val originalImmigrationDetentionId: Int,
  val immigrationDetentionUuid: UUID,
  val prisonerId: String,
  @Enumerated(EnumType.STRING) var immigrationDetentionRecordType: ImmigrationDetentionRecordType,
  var recordDate: LocalDate,
  var homeOfficeReferenceNumber: String?,
  @Enumerated(EnumType.STRING) var noLongerOfInterestReason: ImmigrationDetentionNoLongerOfInterestType?,
  var noLongerOfInterestComment: String?,

  // Audit and status columns
  @Column @Enumerated(EnumType.ORDINAL) val statusId: ImmigrationDetentionEntityStatus,
  val createdAt: ZonedDateTime,
  val createdByUsername: String,
  val createdPrison: String?,
  val updatedAt: ZonedDateTime?,
  val updatedBy: String?,
  val updatedPrison: String?,
  @Enumerated(EnumType.STRING) var source: EventSource = DPS,
  @Column @Enumerated(EnumType.ORDINAL) val historyStatusId: ImmigrationDetentionEntityStatus,
  val historyCreatedAt: ZonedDateTime,
) {

  companion object {
    fun from(original: ImmigrationDetentionEntity, historyStatus: ImmigrationDetentionEntityStatus) = ImmigrationDetentionHistoryEntity(
      id = 0,
      originalImmigrationDetentionId = original.id,
      immigrationDetentionUuid = original.immigrationDetentionUuid,
      prisonerId = original.prisonerId,
      immigrationDetentionRecordType = original.immigrationDetentionRecordType,
      recordDate = original.recordDate,
      homeOfficeReferenceNumber = original.homeOfficeReferenceNumber,
      noLongerOfInterestReason = original.noLongerOfInterestReason,
      noLongerOfInterestComment = original.noLongerOfInterestComment,
      statusId = original.statusId,
      createdAt = original.createdAt,
      createdByUsername = original.createdByUsername,
      createdPrison = original.createdPrison,
      updatedAt = original.updatedAt,
      updatedBy = original.updatedBy,
      updatedPrison = original.updatedPrison,
      source = original.source,
      historyStatusId = historyStatus,
      historyCreatedAt = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS),
    )
  }
}
