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
import jakarta.persistence.ManyToMany
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
  @Column
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Int = 0,
  @Column
  var lifetimeChargeUuid: UUID,
  @Column
  var chargeUuid: UUID,
  @Column
  val offenceCode: String,
  @Column
  val offenceStartDate: LocalDate?,
  @Column
  val offenceEndDate: LocalDate?,
  @Column
  @Enumerated(EnumType.ORDINAL)
  var statusId: EntityStatus,
  @ManyToOne
  @JoinColumn(name = "charge_outcome_id")
  val chargeOutcome: ChargeOutcomeEntity?,
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "superseding_charge_id")
  var supersedingCharge: ChargeEntity?,
  @Column
  val terrorRelated: Boolean?,
  @Column
  val createdAt: ZonedDateTime = ZonedDateTime.now(),
  @Column
  val createdBy: String,
  val createdPrison: String?,
  @JdbcTypeCode(SqlTypes.JSON)
  var legacyData: ChargeLegacyData? = null,
  @ManyToMany(mappedBy = "charges")
  val courtAppearances: MutableSet<CourtAppearanceEntity>,
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "merged_from_case_id")
  val mergedFromCourtCase: CourtCaseEntity? = null,
) {
  @OneToMany(mappedBy = "charge")
  var sentences: MutableList<SentenceEntity> = mutableListOf()

  fun hasNoActiveCourtAppearances(): Boolean = courtAppearances.none { it.statusId == EntityStatus.ACTIVE }

  fun hasTwoOrMoreActiveCourtAppearance(courtAppearance: CourtAppearanceEntity): Boolean = (courtAppearances + courtAppearance).count { it.statusId == EntityStatus.ACTIVE } >= 2

  fun getActiveSentence(): SentenceEntity? = sentences.firstOrNull { it.statusId == EntityStatus.ACTIVE }

  fun getActiveOrInactiveSentence(): SentenceEntity? = sentences.firstOrNull { setOf(EntityStatus.ACTIVE, EntityStatus.INACTIVE).contains(it.statusId) }
  fun isSame(other: ChargeEntity): Boolean = this.offenceCode == other.offenceCode &&
    ((this.offenceStartDate == null && other.offenceStartDate == null) || this.offenceStartDate?.isEqual(other.offenceStartDate) == true) &&
    ((this.offenceEndDate == null && other.offenceEndDate == null) || this.offenceEndDate?.isEqual(other.offenceEndDate) == true) &&
    this.statusId == other.statusId &&
    this.chargeOutcome == other.chargeOutcome &&
    this.terrorRelated == other.terrorRelated &&
    this.legacyData == other.legacyData

  fun copyFrom(charge: LegacyCreateCharge, chargeOutcome: ChargeOutcomeEntity?, createdBy: String): ChargeEntity = ChargeEntity(
    0, lifetimeChargeUuid, UUID.randomUUID(), charge.offenceCode, charge.offenceStartDate, charge.offenceEndDate,
    EntityStatus.ACTIVE, chargeOutcome, this, terrorRelated,
    ZonedDateTime.now(), createdBy, createdPrison, charge.legacyData, courtAppearances.toMutableSet(), mergedFromCourtCase,
  )

  fun copyFrom(charge: LegacyUpdateCharge, chargeOutcome: ChargeOutcomeEntity?, createdBy: String): ChargeEntity = ChargeEntity(
    0, lifetimeChargeUuid, UUID.randomUUID(), offenceCode, charge.offenceStartDate, charge.offenceEndDate,
    EntityStatus.ACTIVE, chargeOutcome, this, terrorRelated,
    ZonedDateTime.now(), createdBy, createdPrison, charge.legacyData, courtAppearances.toMutableSet(), mergedFromCourtCase,
  )

  fun copyFrom(charge: CreateCharge, chargeOutcome: ChargeOutcomeEntity?, createdBy: String): ChargeEntity = ChargeEntity(
    0, lifetimeChargeUuid, UUID.randomUUID(), charge.offenceCode, charge.offenceStartDate, charge.offenceEndDate,
    EntityStatus.ACTIVE, chargeOutcome, this, charge.terrorRelated, ZonedDateTime.now(), createdBy, charge.prisonId, charge.legacyData, courtAppearances.toMutableSet(), mergedFromCourtCase,
  )

  fun copyFrom(chargeOutcome: ChargeOutcomeEntity?, createdBy: String): ChargeEntity {
    val charge = ChargeEntity(
      0, lifetimeChargeUuid, UUID.randomUUID(), offenceCode, offenceStartDate, offenceEndDate,
      EntityStatus.ACTIVE, chargeOutcome, this, terrorRelated, ZonedDateTime.now(), createdBy, createdPrison, legacyData, courtAppearances.toMutableSet(), mergedFromCourtCase,
    )
    charge.sentences = sentences.toMutableList()
    return charge
  }

  fun copyFrom(migrationCreateCharge: MigrationCreateCharge, chargeOutcome: ChargeOutcomeEntity?, createdBy: String, mergedFromCourtCase: CourtCaseEntity?): ChargeEntity = ChargeEntity(
    0, lifetimeChargeUuid, UUID.randomUUID(), migrationCreateCharge.offenceCode, migrationCreateCharge.offenceStartDate, migrationCreateCharge.offenceEndDate,
    if (migrationCreateCharge.merged) EntityStatus.MERGED else EntityStatus.ACTIVE, chargeOutcome, this, null,
    ZonedDateTime.now(), createdBy, null, migrationCreateCharge.legacyData, mutableSetOf(), mergedFromCourtCase,
  )

  fun copyFrom(charge: LegacyUpdateWholeCharge, createdBy: String): ChargeEntity {
    val charge = ChargeEntity(
      0, lifetimeChargeUuid, UUID.randomUUID(), charge.offenceCode, offenceStartDate, offenceEndDate,
      EntityStatus.ACTIVE, chargeOutcome, this, terrorRelated, ZonedDateTime.now(), createdBy, null, legacyData, courtAppearances.toMutableSet(), mergedFromCourtCase,
    )
    charge.sentences = sentences.toMutableList()
    return charge
  }

  companion object {
    fun from(charge: CreateCharge, chargeOutcome: ChargeOutcomeEntity?, createdBy: String): ChargeEntity = ChargeEntity(lifetimeChargeUuid = charge.lifetimeChargeUuid, chargeUuid = charge.chargeUuid, offenceCode = charge.offenceCode, offenceStartDate = charge.offenceStartDate, offenceEndDate = charge.offenceEndDate, statusId = EntityStatus.ACTIVE, chargeOutcome = chargeOutcome, supersedingCharge = null, terrorRelated = charge.terrorRelated, legacyData = charge.legacyData, courtAppearances = mutableSetOf(), createdBy = createdBy, createdPrison = charge.prisonId)

    fun from(charge: LegacyCreateCharge, chargeOutcome: ChargeOutcomeEntity?, createdBy: String): ChargeEntity = ChargeEntity(lifetimeChargeUuid = UUID.randomUUID(), chargeUuid = UUID.randomUUID(), offenceCode = charge.offenceCode, offenceStartDate = charge.offenceStartDate, offenceEndDate = charge.offenceEndDate, statusId = EntityStatus.ACTIVE, chargeOutcome = chargeOutcome, supersedingCharge = null, terrorRelated = null, legacyData = charge.legacyData, courtAppearances = mutableSetOf(), createdBy = createdBy, createdPrison = null)

    fun from(migrationCreateCharge: MigrationCreateCharge, chargeOutcome: ChargeOutcomeEntity?, createdBy: String, mergedFromCourtCase: CourtCaseEntity?): ChargeEntity = ChargeEntity(lifetimeChargeUuid = UUID.randomUUID(), chargeUuid = UUID.randomUUID(), offenceCode = migrationCreateCharge.offenceCode, offenceStartDate = migrationCreateCharge.offenceStartDate, offenceEndDate = migrationCreateCharge.offenceEndDate, statusId = if (migrationCreateCharge.merged) EntityStatus.MERGED else EntityStatus.ACTIVE, chargeOutcome = chargeOutcome, supersedingCharge = null, terrorRelated = null, legacyData = migrationCreateCharge.legacyData, courtAppearances = mutableSetOf(), createdBy = createdBy, createdPrison = null, mergedFromCourtCase = mergedFromCourtCase)
  }
}
