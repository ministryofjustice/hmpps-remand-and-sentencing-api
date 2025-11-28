package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
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
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.CourtAppearanceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.CourtAppearanceLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCreateCourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateCourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.booking.BookingCreateCourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.merge.MergeCreateCourtAppearance
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.*

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
  @Enumerated(EnumType.STRING)
  var statusId: CourtAppearanceEntityStatus,
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
      getStatus(courtAppearance.appearanceDate, courtAppearance.legacyData.appearanceTime, courtAppearance.legacyData.nomisOutcomeCode),
      ZonedDateTime.now(),
      this.createdBy,
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
      getStatus(courtAppearance.appearanceDate, courtAppearance.legacyData?.appearanceTime, courtAppearance.legacyData?.nomisOutcomeCode),
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
    CourtAppearanceEntityStatus.FUTURE,
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

  fun delete(username: String) {
    statusId = CourtAppearanceEntityStatus.DELETED
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
        statusId = getStatus(courtAppearance.appearanceDate, courtAppearance.legacyData?.appearanceTime, courtAppearance.legacyData?.nomisOutcomeCode),
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
      statusId = CourtAppearanceEntityStatus.FUTURE,
      appearanceCharges = mutableSetOf(),
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
        statusId = getStatus(courtAppearance.appearanceDate, courtAppearance.legacyData.appearanceTime, courtAppearance.legacyData.nomisOutcomeCode),
        appearanceCharges = mutableSetOf(),
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
        migrationCreateCourtAppearance.legacyData.nomisOutcomeCode,
      ),
      appearanceCharges = mutableSetOf(),
      createdPrison = null,
      createdBy = createdBy,
      nextCourtAppearance = null,
      warrantType = deriveWarrantType(appearanceOutcome, migrationCreateCourtAppearance.legacyData, migrationCreateCourtAppearance.charges.any { it.sentence != null }),
      overallConvictionDate = null,
      legacyData = migrationCreateCourtAppearance.legacyData,
      source = NOMIS,
    )

    fun from(
      mergeCreateCourtAppearance: MergeCreateCourtAppearance,
      appearanceOutcome: AppearanceOutcomeEntity?,
      courtCase: CourtCaseEntity,
      createdBy: String,
      courtCaseReference: String?,
    ): CourtAppearanceEntity = CourtAppearanceEntity(
      appearanceUuid = UUID.randomUUID(),
      appearanceOutcome = appearanceOutcome,
      courtCase = courtCase,
      courtCode = mergeCreateCourtAppearance.courtCode,
      courtCaseReference = courtCaseReference,
      appearanceDate = mergeCreateCourtAppearance.appearanceDate,
      statusId = getStatus(
        mergeCreateCourtAppearance.appearanceDate,
        mergeCreateCourtAppearance.legacyData.appearanceTime,
        mergeCreateCourtAppearance.legacyData.nomisOutcomeCode,
      ),
      appearanceCharges = mutableSetOf(),
      createdPrison = null,
      createdBy = createdBy,
      nextCourtAppearance = null,
      warrantType = deriveWarrantType(appearanceOutcome, mergeCreateCourtAppearance.legacyData, mergeCreateCourtAppearance.charges.any { it.sentence != null }),
      overallConvictionDate = null,
      legacyData = mergeCreateCourtAppearance.legacyData,
      source = NOMIS,
    )

    fun from(
      bookingCreateCourtAppearance: BookingCreateCourtAppearance,
      appearanceOutcome: AppearanceOutcomeEntity?,
      courtCase: CourtCaseEntity,
      createdBy: String,
      courtCaseReference: String?,
    ): CourtAppearanceEntity = CourtAppearanceEntity(
      appearanceUuid = UUID.randomUUID(),
      appearanceOutcome = appearanceOutcome,
      courtCase = courtCase,
      courtCode = bookingCreateCourtAppearance.courtCode,
      courtCaseReference = courtCaseReference,
      appearanceDate = bookingCreateCourtAppearance.appearanceDate,
      statusId = getStatus(
        bookingCreateCourtAppearance.appearanceDate,
        bookingCreateCourtAppearance.legacyData.appearanceTime,
        bookingCreateCourtAppearance.legacyData.nomisOutcomeCode,
        CourtAppearanceEntityStatus.DUPLICATE,
      ),
      appearanceCharges = mutableSetOf(),
      createdPrison = null,
      createdBy = createdBy,
      nextCourtAppearance = null,
      warrantType = deriveWarrantType(appearanceOutcome, bookingCreateCourtAppearance.legacyData, bookingCreateCourtAppearance.charges.any { it.sentence != null }),
      overallConvictionDate = null,
      legacyData = bookingCreateCourtAppearance.legacyData,
      source = NOMIS,
    )

    private fun getStatus(appearanceDate: LocalDate, appearanceTime: LocalTime?, nomisOutcomeCode: String?, nonFutureStatus: CourtAppearanceEntityStatus = CourtAppearanceEntityStatus.ACTIVE): CourtAppearanceEntityStatus {
      val compareDate = appearanceDate.atTime(appearanceTime ?: LocalTime.MIDNIGHT)
      return when {
        compareDate.isAfter(LocalDateTime.now()) -> CourtAppearanceEntityStatus.FUTURE
        nomisOutcomeCode == RECALL_NOMIS_OUTCOME_CODE -> CourtAppearanceEntityStatus.RECALL_APPEARANCE
        immigrationNomisOutcomeCodes.contains(nomisOutcomeCode) -> CourtAppearanceEntityStatus.IMMIGRATION_APPEARANCE
        else -> nonFutureStatus
      }
    }

    private const val RECALL_NOMIS_OUTCOME_CODE = "1501"
    private val immigrationNomisOutcomeCodes: Set<String> = setOf("5501", "5502")

    private fun deriveWarrantType(
      appearanceOutcome: AppearanceOutcomeEntity?,
      legacyData: CourtAppearanceLegacyData,
      anyChargeHasSentence: Boolean? = null,
    ): String = appearanceOutcome?.warrantType
      ?: if ((legacyData.outcomeConvictionFlag == true && legacyData.outcomeDispositionCode == "F") || anyChargeHasSentence == true) "SENTENCING" else "REMAND"

    fun getLatestCourtAppearance(courtAppearances: Set<CourtAppearanceEntity>): CourtAppearanceEntity? = courtAppearances.filter { it.statusId == CourtAppearanceEntityStatus.ACTIVE }.maxWithOrNull(
      compareBy(
        CourtAppearanceEntity::appearanceDate,
        CourtAppearanceEntity::createdAt,
      ),
    )
  }
}
