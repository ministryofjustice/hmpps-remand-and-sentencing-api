package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateNextCourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.CourtAppearanceLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCreateCourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateCourtAppearance
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.UUID

private const val UNKNOWN_WARRANT_TYPE = "UNKNOWN"

@Entity
@Table(name = "court_appearance")
class CourtAppearanceEntity(
  @Id
  @Column
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Int = 0,
  @Column
  var appearanceUuid: UUID,
  @Column
  var lifetimeUuid: UUID,

  @ManyToOne
  @JoinColumn(name = "appearance_outcome_id")
  val appearanceOutcome: AppearanceOutcomeEntity?,

  @ManyToOne
  @JoinColumn(name = "court_case_id")
  val courtCase: CourtCaseEntity,

  @Column
  val courtCode: String,
  @Column
  val courtCaseReference: String?,
  @Column
  val appearanceDate: LocalDate,
  @Column
  @Enumerated(EnumType.ORDINAL)
  var statusId: EntityStatus,
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "previous_appearance_id")
  var previousAppearance: CourtAppearanceEntity?,

  @Column
  val warrantId: String?,
  @Column
  val createdAt: ZonedDateTime = ZonedDateTime.now(),
  @Column
  val createdByUsername: String,
  @Column
  val createdPrison: String?,
  @Column
  val warrantType: String,
  @Column
  val taggedBail: Int?,
  @ManyToMany
  @JoinTable(
    name = "appearance_charge",
    joinColumns = [JoinColumn(name = "appearance_id")],
    inverseJoinColumns = [JoinColumn(name = "charge_id")],
  )
  val charges: MutableSet<ChargeEntity>,

  @OneToOne
  @JoinColumn(name = "next_court_appearance_id")
  var nextCourtAppearance: NextCourtAppearanceEntity?,

  @Column
  val overallConvictionDate: LocalDate?,
  @JdbcTypeCode(SqlTypes.JSON)
  var legacyData: CourtAppearanceLegacyData? = null,
) {

  @OneToMany
  @JoinColumn(name = "appearance_id")
  var periodLengths: List<PeriodLengthEntity> = emptyList()

  fun isSame(other: CourtAppearanceEntity): Boolean = this.appearanceOutcome == other.appearanceOutcome &&
    this.courtCase == other.courtCase &&
    this.courtCode == other.courtCode &&
    this.courtCaseReference == other.courtCaseReference &&
    this.appearanceDate.isEqual(other.appearanceDate) &&
    this.statusId == other.statusId &&
    this.warrantType == other.warrantType &&
    this.taggedBail == other.taggedBail &&
    periodLengths.all { periodLength -> other.periodLengths.any { otherPeriodLength -> periodLength.isSame(otherPeriodLength) } } &&
    this.overallConvictionDate == other.overallConvictionDate &&
    this.legacyData == other.legacyData &&
    this.createdPrison == other.createdPrison

  fun copyAndRemoveCaseReference(createdByUsername: String): CourtAppearanceEntity {
    val courtAppearance = CourtAppearanceEntity(
      0, UUID.randomUUID(), lifetimeUuid, appearanceOutcome, courtCase, courtCode, null, appearanceDate,
      EntityStatus.ACTIVE, this, warrantId,
      ZonedDateTime.now(), createdByUsername, createdPrison, warrantType, taggedBail, charges.toMutableSet(), nextCourtAppearance, overallConvictionDate, legacyData,
    )
    courtAppearance.periodLengths = periodLengths.toList()
    return courtAppearance
  }

  fun copyFrom(courtAppearance: LegacyCreateCourtAppearance, appearanceOutcome: AppearanceOutcomeEntity?, createdByUsername: String): CourtAppearanceEntity {
    val courtAppearance = CourtAppearanceEntity(
      0, UUID.randomUUID(), lifetimeUuid, appearanceOutcome, courtCase, courtAppearance.courtCode, courtCaseReference, courtAppearance.appearanceDate,
      getStatus(courtAppearance.appearanceDate, courtAppearance.legacyData.appearanceTime), this, warrantId,
      ZonedDateTime.now(), createdByUsername, createdPrison, deriveWarrantType(appearanceOutcome, courtAppearance.legacyData), taggedBail, charges.toMutableSet(), nextCourtAppearance, overallConvictionDate, courtAppearance.legacyData,
    )
    courtAppearance.periodLengths = periodLengths.toList()
    return courtAppearance
  }

  fun copyFrom(courtAppearance: CreateCourtAppearance, appearanceOutcome: AppearanceOutcomeEntity?, courtCase: CourtCaseEntity, createdByUsername: String): CourtAppearanceEntity {
    val courtAppearanceEntity = CourtAppearanceEntity(
      0, UUID.randomUUID(), lifetimeUuid, appearanceOutcome, courtCase, courtAppearance.courtCode, courtAppearance.courtCaseReference, courtAppearance.appearanceDate,
      EntityStatus.ACTIVE, this, courtAppearance.warrantId, ZonedDateTime.now(), createdByUsername, courtAppearance.prisonId, courtAppearance.warrantType, courtAppearance.taggedBail, charges.toMutableSet(), null, courtAppearance.overallConvictionDate, courtAppearance.legacyData,
    )
    courtAppearance.overallSentenceLength?.let { courtAppearanceEntity.periodLengths = listOf(PeriodLengthEntity.from(it)) }
    return courtAppearanceEntity
  }

  fun copyFromFuture(nextCourtAppearance: CreateNextCourtAppearance, courtCase: CourtCaseEntity, createdByUsername: String, courtCaseReference: String?, legacyData: CourtAppearanceLegacyData?): CourtAppearanceEntity = CourtAppearanceEntity(
    0, UUID.randomUUID(), lifetimeUuid, null, courtCase, nextCourtAppearance.courtCode, courtCaseReference, nextCourtAppearance.appearanceDate,
    EntityStatus.FUTURE, this, null,
    ZonedDateTime.now(), createdByUsername, null, UNKNOWN_WARRANT_TYPE, null, mutableSetOf(), null, null, legacyData,
  )

  companion object {

    fun from(courtAppearance: CreateCourtAppearance, appearanceOutcome: AppearanceOutcomeEntity?, courtCase: CourtCaseEntity, createdByUsername: String): CourtAppearanceEntity {
      val courtAppearanceEntity = CourtAppearanceEntity(
        appearanceUuid = courtAppearance.appearanceUuid,
        appearanceOutcome = appearanceOutcome,
        courtCase = courtCase,
        courtCode = courtAppearance.courtCode,
        courtCaseReference = courtAppearance.courtCaseReference,
        appearanceDate = courtAppearance.appearanceDate,
        statusId = EntityStatus.ACTIVE,
        warrantId = courtAppearance.warrantId,
        previousAppearance = null,
        createdPrison = courtAppearance.prisonId,
        createdByUsername = createdByUsername,
        nextCourtAppearance = null,
        warrantType = courtAppearance.warrantType,
        taggedBail = courtAppearance.taggedBail,
        overallConvictionDate = courtAppearance.overallConvictionDate,
        lifetimeUuid = courtAppearance.lifetimeUuid,
        legacyData = courtAppearance.legacyData,
        charges = mutableSetOf(),
      )
      return courtAppearanceEntity
    }

    fun fromFuture(nextCourtAppearance: CreateNextCourtAppearance, courtCase: CourtCaseEntity, createdByUsername: String, courtCaseReference: String?, legacyData: CourtAppearanceLegacyData?): CourtAppearanceEntity = CourtAppearanceEntity(
      appearanceUuid = UUID.randomUUID(),
      appearanceOutcome = null,
      courtCase = courtCase,
      courtCode = nextCourtAppearance.courtCode,
      courtCaseReference = courtCaseReference,
      appearanceDate = nextCourtAppearance.appearanceDate,
      statusId = EntityStatus.FUTURE,
      warrantId = null,
      charges = mutableSetOf(),
      previousAppearance = null,
      createdPrison = null,
      createdByUsername = createdByUsername,
      nextCourtAppearance = null,
      warrantType = UNKNOWN_WARRANT_TYPE,
      taggedBail = null,
      overallConvictionDate = null,
      lifetimeUuid = UUID.randomUUID(),
      legacyData = legacyData,
    )

    fun from(courtAppearance: LegacyCreateCourtAppearance, appearanceOutcome: AppearanceOutcomeEntity?, courtCase: CourtCaseEntity, createdByUsername: String): CourtAppearanceEntity = CourtAppearanceEntity(
      appearanceUuid = UUID.randomUUID(),
      appearanceOutcome = appearanceOutcome,
      courtCase = courtCase,
      courtCode = courtAppearance.courtCode,
      courtCaseReference = null,
      appearanceDate = courtAppearance.appearanceDate,
      statusId = getStatus(courtAppearance.appearanceDate, courtAppearance.legacyData.appearanceTime),
      warrantId = null,
      charges = mutableSetOf(),
      previousAppearance = null,
      createdPrison = null,
      createdByUsername = createdByUsername,
      nextCourtAppearance = null,
      warrantType = deriveWarrantType(appearanceOutcome, courtAppearance.legacyData),
      taggedBail = null,
      overallConvictionDate = null,
      lifetimeUuid = UUID.randomUUID(),
      legacyData = courtAppearance.legacyData,
    )

    fun from(migrationCreateCourtAppearance: MigrationCreateCourtAppearance, appearanceOutcome: AppearanceOutcomeEntity?, courtCase: CourtCaseEntity, createdByUsername: String, courtCaseReference: String?): CourtAppearanceEntity = CourtAppearanceEntity(
      appearanceUuid = UUID.randomUUID(),
      appearanceOutcome = appearanceOutcome,
      courtCase = courtCase,
      courtCode = migrationCreateCourtAppearance.courtCode,
      courtCaseReference = courtCaseReference,
      appearanceDate = migrationCreateCourtAppearance.appearanceDate,
      statusId = getStatus(migrationCreateCourtAppearance.appearanceDate, migrationCreateCourtAppearance.legacyData.appearanceTime),
      warrantId = null,
      charges = mutableSetOf(),
      previousAppearance = null,
      createdPrison = null,
      createdByUsername = createdByUsername,
      nextCourtAppearance = null,
      warrantType = deriveWarrantType(appearanceOutcome, migrationCreateCourtAppearance.legacyData),
      taggedBail = null,
      overallConvictionDate = null,
      lifetimeUuid = UUID.randomUUID(),
      legacyData = migrationCreateCourtAppearance.legacyData,
    )

    private fun getStatus(appearanceDate: LocalDate, appearanceTime: LocalTime?): EntityStatus {
      val compareDate = appearanceDate.atTime(appearanceTime ?: LocalTime.MIDNIGHT)
      return if (compareDate.isAfter(LocalDateTime.now())) EntityStatus.FUTURE else EntityStatus.ACTIVE
    }

    private fun deriveWarrantType(appearanceOutcome: AppearanceOutcomeEntity?, legacyData: CourtAppearanceLegacyData): String = appearanceOutcome?.outcomeType ?: if (legacyData.outcomeConvictionFlag == true && legacyData.outcomeDispositionCode == "F") "SENTENCING" else "REMAND"

    fun getLatestCourtAppearance(courtAppearances: List<CourtAppearanceEntity>): CourtAppearanceEntity? = courtAppearances.filter { it.statusId == EntityStatus.ACTIVE }.maxWithOrNull(
      compareBy(
        CourtAppearanceEntity::appearanceDate,
        CourtAppearanceEntity::createdAt,
      ),
    )
  }
}
