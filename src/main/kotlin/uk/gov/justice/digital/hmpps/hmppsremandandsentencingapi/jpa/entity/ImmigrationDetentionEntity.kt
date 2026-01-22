package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.DynamicUpdate
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateImmigrationDetention
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.EventSource
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.EventSource.DPS
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ImmigrationDetentionEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ImmigrationDetentionEntityStatus.ACTIVE
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ImmigrationDetentionNoLongerOfInterestType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ImmigrationDetentionRecordType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCreateCourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.util.ImmigrationDetentionRecordTypeOutcomeMapper
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

@Entity
@Table(name = "immigration_detention")
@DynamicUpdate
class ImmigrationDetentionEntity(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Int = 0,
  val immigrationDetentionUuid: UUID = UUID.randomUUID(),
  var prisonerId: String,
  @Enumerated(EnumType.STRING)
  var immigrationDetentionRecordType: ImmigrationDetentionRecordType,
  var recordDate: LocalDate,
  var homeOfficeReferenceNumber: String?,
  @Enumerated(EnumType.STRING)
  var noLongerOfInterestReason: ImmigrationDetentionNoLongerOfInterestType?,
  var noLongerOfInterestComment: String?,
  var courtAppearanceUuid: UUID? = null,

  // Audit and status columns
  @Column
  @Enumerated(EnumType.STRING)
  var statusId: ImmigrationDetentionEntityStatus,
  val createdAt: ZonedDateTime = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS),
  val createdByUsername: String,
  val createdPrison: String? = null,
  var updatedAt: ZonedDateTime? = null,
  var updatedBy: String? = null,
  var updatedPrison: String? = null,
  @Enumerated(EnumType.STRING)
  var source: EventSource = DPS,
) {

  fun delete(username: String) {
    statusId = ImmigrationDetentionEntityStatus.DELETED
    updatedAt = ZonedDateTime.now()
    updatedBy = username
  }

  fun copyFrom(
    courtAppearance: CourtAppearanceEntity,
    updateRequest: LegacyCreateCourtAppearance,
    performedByUser: String,
  ): ImmigrationDetentionEntity = ImmigrationDetentionEntity(
    0,
    UUID.randomUUID(),
    courtAppearance.courtCase.prisonerId,
    ImmigrationDetentionRecordTypeOutcomeMapper.outcomeToRecordType(courtAppearance.appearanceOutcome),
    updateRequest.appearanceDate,
    homeOfficeReferenceNumber,
    noLongerOfInterestReason,
    noLongerOfInterestComment,
    courtAppearance.appearanceUuid,
    ACTIVE,
    ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS),
    performedByUser,
  )

  fun isSame(other: ImmigrationDetentionEntity): Boolean = recordDate == other.recordDate

  fun updateFrom(updatedImmigrationDetentionRecord: ImmigrationDetentionEntity) {
    this.prisonerId = updatedImmigrationDetentionRecord.prisonerId
    this.immigrationDetentionRecordType = updatedImmigrationDetentionRecord.immigrationDetentionRecordType
    this.recordDate = updatedImmigrationDetentionRecord.recordDate
    this.homeOfficeReferenceNumber = updatedImmigrationDetentionRecord.homeOfficeReferenceNumber
    this.noLongerOfInterestReason = updatedImmigrationDetentionRecord.noLongerOfInterestReason
    this.noLongerOfInterestComment = updatedImmigrationDetentionRecord.noLongerOfInterestComment
    this.updatedAt = updatedImmigrationDetentionRecord.createdAt
    this.updatedBy = updatedImmigrationDetentionRecord.createdByUsername
  }

  companion object {
    fun fromDPS(
      create: CreateImmigrationDetention,
      immigrationDetentionUuid: UUID? = null,
      courtAppearanceUuid: UUID? = null,
    ): ImmigrationDetentionEntity = ImmigrationDetentionEntity(
      immigrationDetentionUuid = immigrationDetentionUuid ?: UUID.randomUUID(),
      prisonerId = create.prisonerId,
      immigrationDetentionRecordType = create.immigrationDetentionRecordType,
      recordDate = create.recordDate,
      homeOfficeReferenceNumber = create.homeOfficeReferenceNumber,
      noLongerOfInterestReason = create.noLongerOfInterestReason,
      noLongerOfInterestComment = create.noLongerOfInterestComment,
      courtAppearanceUuid = courtAppearanceUuid,
      statusId = ACTIVE,
      createdByUsername = create.createdByUsername,
      source = DPS,
    )
  }
}
