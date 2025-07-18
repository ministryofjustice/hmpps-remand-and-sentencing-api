package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.annotations.BatchSize
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateNextCourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.EventSource
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.EventSource.DPS
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.EventSource.NOMIS
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
  val appearanceUuid: UUID,

  @ManyToOne
  @JoinColumn(name = "appearance_outcome_id")
  var appearanceOutcome: AppearanceOutcomeEntity?,

  @ManyToOne
  @JoinColumn(name = "court_case_id")
  val courtCase: CourtCaseEntity,

  @Column
  var courtCode: String,
  @Column
  var courtCaseReference: String?,
  @Column
  var appearanceDate: LocalDate,
  @Column
  @Enumerated(EnumType.ORDINAL)
  var statusId: EntityStatus,
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "previous_appearance_id")
  var previousAppearance: CourtAppearanceEntity?,

  @Column
  var warrantId: String?,
  @Column
  val createdAt: ZonedDateTime = ZonedDateTime.now(),
  @Column
  val createdBy: String,
  @Column
  val createdPrison: String?,
  var updatedAt: ZonedDateTime? = null,
  var updatedBy: String? = null,
  var updatedPrison: String? = null,
  @Column
  var warrantType: String,

  @OneToMany(mappedBy = "appearance", cascade = [CascadeType.ALL], orphanRemoval = true)
  @BatchSize(size = 50)
  val appearanceCharges: MutableSet<AppearanceChargeEntity> = mutableSetOf(),

  @OneToOne
  @JoinColumn(name = "next_court_appearance_id")
  var nextCourtAppearance: NextCourtAppearanceEntity?,

  @Column
  var overallConvictionDate: LocalDate?,
  @JdbcTypeCode(SqlTypes.JSON)
  var legacyData: CourtAppearanceLegacyData? = null,

  @OneToMany(mappedBy = "appearance", cascade = [CascadeType.ALL], orphanRemoval = true)
  val documents: MutableSet<UploadedDocumentEntity> = mutableSetOf(),

  @Enumerated(EnumType.STRING)
  var source: EventSource = DPS,
) {

  @OneToMany
  @JoinColumn(name = "appearance_id")
  var periodLengths: MutableSet<PeriodLengthEntity> = mutableSetOf()

  fun isSame(other: CourtAppearanceEntity): Boolean = this.appearanceOutcome == other.appearanceOutcome &&
    this.courtCase == other.courtCase &&
    this.courtCode == other.courtCode &&
    this.courtCaseReference == other.courtCaseReference &&
    this.appearanceDate.isEqual(other.appearanceDate) &&
    this.statusId == other.statusId &&
    this.warrantType == other.warrantType &&
    this.overallConvictionDate == other.overallConvictionDate &&
    this.legacyData == other.legacyData &&
    this.createdPrison == other.createdPrison

  fun copyFrom(
    courtAppearance: LegacyCreateCourtAppearance,
    appearanceOutcome: AppearanceOutcomeEntity?,
    createdBy: String,
  ): CourtAppearanceEntity {
    val courtAppearance = CourtAppearanceEntity(
      0,
      UUID.randomUUID(),
      appearanceOutcome,
      courtCase,
      courtAppearance.courtCode,
      courtCaseReference,
      courtAppearance.appearanceDate,
      getStatus(courtAppearance.appearanceDate, courtAppearance.legacyData.appearanceTime),
      this,
      warrantId,
      ZonedDateTime.now(),
      createdBy,
      createdPrison,
      ZonedDateTime.now(),
      createdBy,
      createdPrison,
      deriveWarrantType(appearanceOutcome, courtAppearance.legacyData),
      appearanceCharges.toMutableSet(),
      nextCourtAppearance,
      overallConvictionDate,
      courtAppearance.legacyData,
      documents.toMutableSet(),
      source = source,
    )
    courtAppearance.periodLengths = periodLengths.toMutableSet()
    return courtAppearance
  }

  fun copyFrom(
    courtAppearance: CreateCourtAppearance,
    appearanceOutcome: AppearanceOutcomeEntity?,
    courtCase: CourtCaseEntity,
    createdBy: String,
  ): CourtAppearanceEntity {
    val courtAppearanceEntity = CourtAppearanceEntity(
      0,
      UUID.randomUUID(),
      appearanceOutcome,
      courtCase,
      courtAppearance.courtCode,
      courtAppearance.courtCaseReference,
      courtAppearance.appearanceDate,
      EntityStatus.ACTIVE,
      this,
      courtAppearance.warrantId,
      ZonedDateTime.now(),
      createdBy,
      courtAppearance.prisonId,
      ZonedDateTime.now(),
      createdBy,
      courtAppearance.prisonId,
      courtAppearance.warrantType,
      appearanceCharges.toMutableSet(),
      null,
      courtAppearance.overallConvictionDate,
      courtAppearance.legacyData,
      documents.toMutableSet(),
      source = source,
    )
    courtAppearance.overallSentenceLength?.let {
      courtAppearanceEntity.periodLengths = mutableSetOf(PeriodLengthEntity.from(it, createdBy))
    }
    return courtAppearanceEntity
  }

  fun copyFromFuture(
    nextCourtAppearance: CreateNextCourtAppearance,
    courtCase: CourtCaseEntity,
    createdBy: String,
    courtCaseReference: String?,
    legacyData: CourtAppearanceLegacyData?,
  ): CourtAppearanceEntity = CourtAppearanceEntity(
    0,
    UUID.randomUUID(),
    null,
    courtCase,
    nextCourtAppearance.courtCode,
    courtCaseReference,
    nextCourtAppearance.appearanceDate,
    EntityStatus.FUTURE,
    this,
    null,
    ZonedDateTime.now(),
    createdBy,
    nextCourtAppearance.prisonId,
    ZonedDateTime.now(),
    createdBy,
    nextCourtAppearance.prisonId,
    UNKNOWN_WARRANT_TYPE,
    mutableSetOf(),
    null,
    null,
    legacyData,
  )

  fun updateFrom(courtAppearanceEntity: CourtAppearanceEntity) {
    appearanceOutcome = courtAppearanceEntity.appearanceOutcome
    courtCode = courtAppearanceEntity.courtCode
    courtCaseReference = courtAppearanceEntity.courtCaseReference
    appearanceDate = courtAppearanceEntity.appearanceDate
    statusId = courtAppearanceEntity.statusId
    previousAppearance = courtAppearanceEntity.previousAppearance
    warrantId = courtAppearanceEntity.warrantId
    updatedAt = courtAppearanceEntity.updatedAt
    updatedBy = courtAppearanceEntity.updatedBy
    updatedPrison = courtAppearanceEntity.updatedPrison
    warrantType = courtAppearanceEntity.warrantType
    overallConvictionDate = courtAppearanceEntity.overallConvictionDate
    legacyData = courtAppearanceEntity.legacyData
  }

  fun updatedAndRemoveCaseReference(username: String) {
    courtCaseReference = null
    updatedBy = username
    updatedAt = ZonedDateTime.now()
  }

  fun updateNextCourtAppearance(username: String, nextCourtAppearance: NextCourtAppearanceEntity) {
    this.nextCourtAppearance = nextCourtAppearance
    updatedBy = username
    updatedAt = ZonedDateTime.now()
  }

  fun delete(username: String) {
    statusId = EntityStatus.DELETED
    updatedAt = ZonedDateTime.now()
    updatedBy = username
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as CourtAppearanceEntity

    if (id != other.id) return false
    if (appearanceUuid != other.appearanceUuid) return false

    return true
  }

  override fun hashCode(): Int {
    var result = id
    result = 31 * result + appearanceUuid.hashCode()
    return result
  }

  companion object {

    fun from(
      courtAppearance: CreateCourtAppearance,
      appearanceOutcome: AppearanceOutcomeEntity?,
      courtCase: CourtCaseEntity,
      createdBy: String,
    ): CourtAppearanceEntity {
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
        createdBy = createdBy,
        nextCourtAppearance = null,
        warrantType = courtAppearance.warrantType,
        overallConvictionDate = courtAppearance.overallConvictionDate,
        legacyData = courtAppearance.legacyData,
        appearanceCharges = mutableSetOf(),
      )
      return courtAppearanceEntity
    }

    fun fromFuture(
      nextCourtAppearance: CreateNextCourtAppearance,
      courtCase: CourtCaseEntity,
      createdBy: String,
      courtCaseReference: String?,
      legacyData: CourtAppearanceLegacyData?,
    ): CourtAppearanceEntity = CourtAppearanceEntity(
      appearanceUuid = UUID.randomUUID(),
      appearanceOutcome = null,
      courtCase = courtCase,
      courtCode = nextCourtAppearance.courtCode,
      courtCaseReference = courtCaseReference,
      appearanceDate = nextCourtAppearance.appearanceDate,
      statusId = EntityStatus.FUTURE,
      warrantId = null,
      appearanceCharges = mutableSetOf(),
      previousAppearance = null,
      createdPrison = nextCourtAppearance.prisonId,
      createdBy = createdBy,
      nextCourtAppearance = null,
      warrantType = UNKNOWN_WARRANT_TYPE,
      overallConvictionDate = null,
      legacyData = legacyData,
    )

    fun from(
      courtAppearance: LegacyCreateCourtAppearance,
      appearanceOutcome: AppearanceOutcomeEntity?,
      courtCase: CourtCaseEntity,
      createdBy: String,
    ): CourtAppearanceEntity {
      val courtCaseReference = courtCase.legacyData?.caseReferences?.maxByOrNull { it.updatedDate }?.offenderCaseReference
      return CourtAppearanceEntity(
        appearanceUuid = UUID.randomUUID(),
        appearanceOutcome = appearanceOutcome,
        courtCase = courtCase,
        courtCode = courtAppearance.courtCode,
        courtCaseReference = courtCaseReference,
        appearanceDate = courtAppearance.appearanceDate,
        statusId = getStatus(courtAppearance.appearanceDate, courtAppearance.legacyData.appearanceTime),
        warrantId = null,
        appearanceCharges = mutableSetOf(),
        previousAppearance = null,
        createdPrison = null,
        createdBy = createdBy,
        nextCourtAppearance = null,
        warrantType = deriveWarrantType(appearanceOutcome, courtAppearance.legacyData),
        overallConvictionDate = null,
        legacyData = courtAppearance.legacyData,
        source = NOMIS,
      )
    }

    fun from(
      migrationCreateCourtAppearance: MigrationCreateCourtAppearance,
      appearanceOutcome: AppearanceOutcomeEntity?,
      courtCase: CourtCaseEntity,
      createdBy: String,
      courtCaseReference: String?,
    ): CourtAppearanceEntity = CourtAppearanceEntity(
      appearanceUuid = UUID.randomUUID(),
      appearanceOutcome = appearanceOutcome,
      courtCase = courtCase,
      courtCode = migrationCreateCourtAppearance.courtCode,
      courtCaseReference = courtCaseReference,
      appearanceDate = migrationCreateCourtAppearance.appearanceDate,
      statusId = getStatus(
        migrationCreateCourtAppearance.appearanceDate,
        migrationCreateCourtAppearance.legacyData.appearanceTime,
      ),
      warrantId = null,
      appearanceCharges = mutableSetOf(),
      previousAppearance = null,
      createdPrison = null,
      createdBy = createdBy,
      nextCourtAppearance = null,
      warrantType = deriveWarrantType(appearanceOutcome, migrationCreateCourtAppearance.legacyData, migrationCreateCourtAppearance.charges.any { it.sentence != null }),
      overallConvictionDate = null,
      legacyData = migrationCreateCourtAppearance.legacyData,
      source = NOMIS,
    )

    private fun getStatus(appearanceDate: LocalDate, appearanceTime: LocalTime?): EntityStatus {
      val compareDate = appearanceDate.atTime(appearanceTime ?: LocalTime.MIDNIGHT)
      return if (compareDate.isAfter(LocalDateTime.now())) EntityStatus.FUTURE else EntityStatus.ACTIVE
    }

    private fun deriveWarrantType(
      appearanceOutcome: AppearanceOutcomeEntity?,
      legacyData: CourtAppearanceLegacyData,
      anyChargeHasSentence: Boolean? = null,
    ): String = appearanceOutcome?.outcomeType
      ?: if ((legacyData.outcomeConvictionFlag == true && legacyData.outcomeDispositionCode == "F") || anyChargeHasSentence == true) "SENTENCING" else "REMAND"

    fun getLatestCourtAppearance(courtAppearances: Set<CourtAppearanceEntity>): CourtAppearanceEntity? = courtAppearances.filter { it.statusId == EntityStatus.ACTIVE }.maxWithOrNull(
      compareBy(
        CourtAppearanceEntity::appearanceDate,
        CourtAppearanceEntity::createdAt,
      ),
    )
  }
}
