package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity

import jakarta.persistence.CascadeType
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
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCharge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.ChargeLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCreateCharge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyUpdateCharge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyUpdateWholeCharge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateCharge
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

@Entity
@Table(name = "charge")
class ChargeEntity(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Int = 0,
  val chargeUuid: UUID,
  var offenceCode: String,
  var offenceStartDate: LocalDate?,
  var offenceEndDate: LocalDate?,
  @Enumerated(EnumType.ORDINAL)
  var statusId: EntityStatus,
  @ManyToOne
  @JoinColumn(name = "charge_outcome_id")
  var chargeOutcome: ChargeOutcomeEntity?,
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "superseding_charge_id")
  var supersedingCharge: ChargeEntity?,
  var terrorRelated: Boolean?,
  val createdAt: ZonedDateTime = ZonedDateTime.now(),
  val createdBy: String,
  val createdPrison: String?,
  var updatedAt: ZonedDateTime = createdAt,
  var updatedBy: String? = null,
  var updatedPrison: String? = null,
  @JdbcTypeCode(SqlTypes.JSON)
  var legacyData: ChargeLegacyData? = null,

  @OneToMany(mappedBy = "charge", cascade = [CascadeType.ALL], orphanRemoval = true)
  val appearanceCharges: MutableSet<AppearanceChargeEntity> = mutableSetOf(),

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "merged_from_case_id")
  var mergedFromCourtCase: CourtCaseEntity? = null,
) {
  @OneToMany(mappedBy = "charge")
  var sentences: MutableSet<SentenceEntity> = mutableSetOf()

  fun hasNoActiveCourtAppearances(): Boolean = appearanceCharges.none { it.appearance!!.statusId == EntityStatus.ACTIVE }

  fun hasTwoOrMoreActiveCourtAppearance(courtAppearance: CourtAppearanceEntity): Boolean = (appearanceCharges.map { it.appearance!! } + courtAppearance).toSet().count { it.statusId == EntityStatus.ACTIVE } >= 2

  fun getActiveSentence(): SentenceEntity? = sentences.firstOrNull { it.statusId == EntityStatus.ACTIVE }

  fun getActiveOrInactiveSentence(): SentenceEntity? = sentences.firstOrNull { setOf(EntityStatus.ACTIVE, EntityStatus.INACTIVE).contains(it.statusId) }
  fun isSame(other: ChargeEntity): Boolean = this.offenceCode == other.offenceCode &&
    ((this.offenceStartDate == null && other.offenceStartDate == null) || (other.offenceStartDate != null && this.offenceStartDate?.isEqual(other.offenceStartDate) == true)) &&
    ((this.offenceEndDate == null && other.offenceEndDate == null) || (other.offenceEndDate != null && this.offenceEndDate?.isEqual(other.offenceEndDate) == true)) &&
    this.statusId == other.statusId &&
    this.chargeOutcome == other.chargeOutcome &&
    this.terrorRelated == other.terrorRelated &&
    this.legacyData == other.legacyData

  fun copyFrom(charge: LegacyCreateCharge, chargeOutcome: ChargeOutcomeEntity?, createdBy: String): ChargeEntity = ChargeEntity(
    0, chargeUuid, charge.offenceCode, charge.offenceStartDate, charge.offenceEndDate,
    EntityStatus.ACTIVE, chargeOutcome, this, terrorRelated,
    createdAt, this.createdBy, createdPrison, ZonedDateTime.now(), createdBy, updatedPrison ?: createdPrison, charge.legacyData,
    appearanceCharges.toMutableSet(),
    mergedFromCourtCase,
  )

  fun copyFrom(charge: LegacyUpdateCharge, chargeOutcome: ChargeOutcomeEntity?, createdBy: String): ChargeEntity = ChargeEntity(
    0, chargeUuid, offenceCode, charge.offenceStartDate, charge.offenceEndDate,
    EntityStatus.ACTIVE, chargeOutcome, this, terrorRelated,
    createdAt, this.createdBy, createdPrison, ZonedDateTime.now(), createdBy, updatedPrison ?: createdPrison, charge.legacyData,
    appearanceCharges.toMutableSet(),
    mergedFromCourtCase,
  )

  fun copyFrom(charge: CreateCharge, chargeOutcome: ChargeOutcomeEntity?, createdBy: String): ChargeEntity = ChargeEntity(
    0, chargeUuid, charge.offenceCode, charge.offenceStartDate, charge.offenceEndDate,
    EntityStatus.ACTIVE, chargeOutcome, this, charge.terrorRelated,
    createdAt, this.createdBy, charge.prisonId, ZonedDateTime.now(), createdBy, charge.prisonId,
    charge.legacyData, appearanceCharges.toMutableSet(),
    mergedFromCourtCase,
  )

  fun copyFrom(chargeOutcome: ChargeOutcomeEntity?, createdBy: String): ChargeEntity {
    val charge = ChargeEntity(
      0, chargeUuid, offenceCode, offenceStartDate, offenceEndDate,
      EntityStatus.ACTIVE, chargeOutcome, this, terrorRelated,
      createdAt, this.createdBy, createdPrison, ZonedDateTime.now(), createdBy, updatedPrison ?: createdPrison,
      legacyData, appearanceCharges.toMutableSet(),
      mergedFromCourtCase,
    )
    charge.sentences = sentences.toMutableSet()
    return charge
  }

  fun copyFrom(migrationCreateCharge: MigrationCreateCharge, chargeOutcome: ChargeOutcomeEntity?, createdBy: String): ChargeEntity = ChargeEntity(
    0, chargeUuid, migrationCreateCharge.offenceCode, migrationCreateCharge.offenceStartDate, migrationCreateCharge.offenceEndDate,
    if (migrationCreateCharge.merged) EntityStatus.MERGED else EntityStatus.ACTIVE, chargeOutcome, this, null,
    createdAt, this.createdBy, null, ZonedDateTime.now(), createdBy, updatedPrison ?: createdPrison, migrationCreateCharge.legacyData, mutableSetOf(), null,
  )

  fun copyFrom(charge: LegacyUpdateWholeCharge, createdBy: String): ChargeEntity {
    val chargeEntity = ChargeEntity(
      0, chargeUuid, charge.offenceCode, offenceStartDate, offenceEndDate,
      EntityStatus.ACTIVE, chargeOutcome, this, terrorRelated,
      createdAt, this.createdBy, null, ZonedDateTime.now(), createdBy, updatedPrison ?: createdPrison,
      legacyData, appearanceCharges.toMutableSet(),
      mergedFromCourtCase,
    )
    chargeEntity.sentences = sentences.toMutableSet()
    return chargeEntity
  }

  fun copyFromReplacedCharge(replacedCharge: ChargeEntity): ChargeEntity {
    val currentDate = ZonedDateTime.now()
    val charge = ChargeEntity(
      0, UUID.randomUUID(), offenceCode, offenceStartDate, offenceEndDate, EntityStatus.ACTIVE, chargeOutcome, replacedCharge,
      terrorRelated,
      currentDate, createdBy, createdPrison, currentDate, null, null, legacyData, appearanceCharges.toMutableSet(),
    )
    charge.sentences = sentences.toMutableSet()
    return charge
  }

  fun updateFrom(chargeEntity: ChargeEntity) {
    offenceCode = chargeEntity.offenceCode
    offenceStartDate = chargeEntity.offenceStartDate
    offenceEndDate = chargeEntity.offenceEndDate
    statusId = chargeEntity.statusId
    chargeOutcome = chargeEntity.chargeOutcome
    supersedingCharge = chargeEntity.supersedingCharge
    terrorRelated = chargeEntity.terrorRelated
    updatedAt = chargeEntity.updatedAt
    updatedBy = chargeEntity.updatedBy
    updatedPrison = chargeEntity.updatedPrison
    legacyData = chargeEntity.legacyData
    mergedFromCourtCase = chargeEntity.mergedFromCourtCase
  }

  fun updateFrom(chargeOutcome: ChargeOutcomeEntity?, username: String, prisonId: String) {
    this.chargeOutcome = chargeOutcome
    updatedAt = ZonedDateTime.now()
    updatedBy = username
    updatedPrison = prisonId
  }

  fun delete(deletedBy: String) {
    statusId = EntityStatus.DELETED
    updatedAt = ZonedDateTime.now()
    updatedBy = deletedBy
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as ChargeEntity

    if (id != other.id) return false
    if (chargeUuid != other.chargeUuid) return false

    return true
  }

  override fun hashCode(): Int {
    var result = id
    result = 31 * result + chargeUuid.hashCode()
    return result
  }

  companion object {
    fun from(charge: CreateCharge, chargeOutcome: ChargeOutcomeEntity?, createdBy: String): ChargeEntity = ChargeEntity(chargeUuid = charge.chargeUuid, offenceCode = charge.offenceCode, offenceStartDate = charge.offenceStartDate, offenceEndDate = charge.offenceEndDate, statusId = EntityStatus.ACTIVE, chargeOutcome = chargeOutcome, supersedingCharge = null, terrorRelated = charge.terrorRelated, legacyData = charge.legacyData, appearanceCharges = mutableSetOf(), createdBy = createdBy, createdPrison = charge.prisonId)

    fun from(charge: LegacyCreateCharge, chargeOutcome: ChargeOutcomeEntity?, createdBy: String): ChargeEntity = ChargeEntity(chargeUuid = UUID.randomUUID(), offenceCode = charge.offenceCode, offenceStartDate = charge.offenceStartDate, offenceEndDate = charge.offenceEndDate, statusId = EntityStatus.ACTIVE, chargeOutcome = chargeOutcome, supersedingCharge = null, terrorRelated = null, legacyData = charge.legacyData, appearanceCharges = mutableSetOf(), createdBy = createdBy, createdPrison = null)

    fun from(migrationCreateCharge: MigrationCreateCharge, chargeOutcome: ChargeOutcomeEntity?, createdBy: String): ChargeEntity = ChargeEntity(chargeUuid = UUID.randomUUID(), offenceCode = migrationCreateCharge.offenceCode, offenceStartDate = migrationCreateCharge.offenceStartDate, offenceEndDate = migrationCreateCharge.offenceEndDate, statusId = if (migrationCreateCharge.merged) EntityStatus.MERGED else EntityStatus.ACTIVE, chargeOutcome = chargeOutcome, supersedingCharge = null, terrorRelated = null, legacyData = migrationCreateCharge.legacyData, appearanceCharges = mutableSetOf(), createdBy = createdBy, createdPrison = null)
  }
}
