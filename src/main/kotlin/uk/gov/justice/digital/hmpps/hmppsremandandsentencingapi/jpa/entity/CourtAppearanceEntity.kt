package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity

import com.fasterxml.jackson.databind.JsonNode
import io.hypersistence.utils.hibernate.type.json.JsonType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
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
import org.hibernate.annotations.Type
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateNextCourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCreateCourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateCourtAppearance
import java.time.LocalDate
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
  @OneToOne
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
  @Type(value = JsonType::class)
  @Column(columnDefinition = "jsonb")
  var legacyData: JsonNode? = null,
) {

  @OneToMany
  @JoinColumn(name = "appearance_id")
  var periodLengths: List<PeriodLengthEntity> = emptyList()

  fun isSame(other: CourtAppearanceEntity): Boolean {
    return this.appearanceOutcome == other.appearanceOutcome &&
      this.courtCase == other.courtCase &&
      this.courtCode == other.courtCode &&
      this.courtCaseReference == other.courtCaseReference &&
      this.appearanceDate.isEqual(other.appearanceDate) &&
      this.statusId == other.statusId &&
      this.warrantType == other.warrantType &&
      this.taggedBail == other.taggedBail &&
      periodLengths.all { periodLength -> other.periodLengths.any { otherPeriodLength -> periodLength.isSame(otherPeriodLength) } } &&
      this.overallConvictionDate == other.overallConvictionDate &&
      this.legacyData == other.legacyData
  }

  fun copyAndRemoveCaseReference(createdByUsername: String): CourtAppearanceEntity {
    val courtAppearance = CourtAppearanceEntity(
      0, UUID.randomUUID(), lifetimeUuid, appearanceOutcome, courtCase, courtCode, null, appearanceDate,
      EntityStatus.ACTIVE, this, warrantId,
      ZonedDateTime.now(), createdByUsername, createdPrison, warrantType, taggedBail, charges.toMutableSet(), nextCourtAppearance, overallConvictionDate, legacyData,
    )
    courtAppearance.periodLengths = periodLengths.toList()
    return courtAppearance
  }

  fun copyFrom(courtAppearance: LegacyCreateCourtAppearance, appearanceOutcome: AppearanceOutcomeEntity?, createdByUsername: String, legacyData: JsonNode?): CourtAppearanceEntity {
    val courtAppearance = CourtAppearanceEntity(
      0, UUID.randomUUID(), lifetimeUuid, appearanceOutcome, courtCase, courtAppearance.courtCode, courtCaseReference, courtAppearance.appearanceDate,
      EntityStatus.ACTIVE, this, warrantId,
      ZonedDateTime.now(), createdByUsername, createdPrison, appearanceOutcome?.outcomeType ?: UNKNOWN_WARRANT_TYPE, taggedBail, charges.toMutableSet(), nextCourtAppearance, overallConvictionDate, legacyData,
    )
    courtAppearance.periodLengths = periodLengths.toList()
    return courtAppearance
  }

  fun copyFrom(courtAppearance: CreateCourtAppearance, appearanceOutcome: AppearanceOutcomeEntity?, courtCase: CourtCaseEntity, createdByUsername: String, legacyData: JsonNode?): CourtAppearanceEntity {
    val courtAppearanceEntity = CourtAppearanceEntity(
      0, UUID.randomUUID(), lifetimeUuid, appearanceOutcome, courtCase, courtAppearance.courtCode, courtAppearance.courtCaseReference, courtAppearance.appearanceDate,
      EntityStatus.ACTIVE, this, courtAppearance.warrantId, ZonedDateTime.now(), createdByUsername, null, courtAppearance.warrantType, courtAppearance.taggedBail, charges.toMutableSet(), null, courtAppearance.overallConvictionDate, legacyData,
    )
    courtAppearance.overallSentenceLength?.let { courtAppearanceEntity.periodLengths = listOf(PeriodLengthEntity.from(it)) }
    return courtAppearanceEntity
  }

  fun copyFromFuture(nextCourtAppearance: CreateNextCourtAppearance, courtCase: CourtCaseEntity, createdByUsername: String, courtCaseReference: String?): CourtAppearanceEntity {
    return CourtAppearanceEntity(
      0, UUID.randomUUID(), lifetimeUuid, null, courtCase, nextCourtAppearance.courtCode, courtCaseReference, nextCourtAppearance.appearanceDate,
      EntityStatus.FUTURE, this, null,
      ZonedDateTime.now(), createdByUsername, null, UNKNOWN_WARRANT_TYPE, null, mutableSetOf(), null, null, null,
    )
  }

  companion object {

    fun from(courtAppearance: CreateCourtAppearance, appearanceOutcome: AppearanceOutcomeEntity?, courtCase: CourtCaseEntity, createdByUsername: String, legacyData: JsonNode?): CourtAppearanceEntity {
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
        createdPrison = null,
        createdByUsername = createdByUsername,
        nextCourtAppearance = null,
        warrantType = courtAppearance.warrantType,
        taggedBail = courtAppearance.taggedBail,
        overallConvictionDate = courtAppearance.overallConvictionDate,
        lifetimeUuid = courtAppearance.lifetimeUuid,
        legacyData = legacyData,
        charges = mutableSetOf(),
      )
      return courtAppearanceEntity
    }

    fun fromFuture(nextCourtAppearance: CreateNextCourtAppearance, courtCase: CourtCaseEntity, createdByUsername: String, courtCaseReference: String?): CourtAppearanceEntity {
      return CourtAppearanceEntity(
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
        legacyData = null,
      )
    }

    fun from(courtAppearance: LegacyCreateCourtAppearance, appearanceOutcome: AppearanceOutcomeEntity?, courtCase: CourtCaseEntity, createdByUsername: String, legacyData: JsonNode?): CourtAppearanceEntity {
      return CourtAppearanceEntity(
        appearanceUuid = UUID.randomUUID(),
        appearanceOutcome = appearanceOutcome,
        courtCase = courtCase,
        courtCode = courtAppearance.courtCode,
        courtCaseReference = null,
        appearanceDate = courtAppearance.appearanceDate,
        statusId = EntityStatus.ACTIVE,
        warrantId = null,
        charges = mutableSetOf(),
        previousAppearance = null,
        createdPrison = null,
        createdByUsername = createdByUsername,
        nextCourtAppearance = null,
        warrantType = appearanceOutcome?.outcomeType ?: UNKNOWN_WARRANT_TYPE,
        taggedBail = null,
        overallConvictionDate = null,
        lifetimeUuid = UUID.randomUUID(),
        legacyData = legacyData,
      )
    }

    fun from(migrationCreateCourtAppearance: MigrationCreateCourtAppearance, appearanceOutcome: AppearanceOutcomeEntity?, courtCase: CourtCaseEntity, createdByUsername: String, legacyData: JsonNode, courtCaseReference: String?): CourtAppearanceEntity {
      return CourtAppearanceEntity(
        appearanceUuid = UUID.randomUUID(),
        appearanceOutcome = appearanceOutcome,
        courtCase = courtCase,
        courtCode = migrationCreateCourtAppearance.courtCode,
        courtCaseReference = courtCaseReference,
        appearanceDate = migrationCreateCourtAppearance.appearanceDate,
        statusId = if (migrationCreateCourtAppearance.appearanceDate.isAfter(LocalDate.now())) EntityStatus.FUTURE else EntityStatus.ACTIVE,
        warrantId = null,
        charges = mutableSetOf(),
        previousAppearance = null,
        createdPrison = null,
        createdByUsername = createdByUsername,
        nextCourtAppearance = null,
        warrantType = appearanceOutcome?.outcomeType ?: UNKNOWN_WARRANT_TYPE,
        taggedBail = null,
        overallConvictionDate = null,
        lifetimeUuid = UUID.randomUUID(),
        legacyData = legacyData,
      )
    }

    fun getLatestCourtAppearance(courtAppearances: List<CourtAppearanceEntity>): CourtAppearanceEntity? {
      return courtAppearances.filter { it.statusId == EntityStatus.ACTIVE }.maxByOrNull { it.appearanceDate }
    }
  }
}
