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
import jakarta.persistence.ManyToMany
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.annotations.Type
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCharge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
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
  @OneToOne
  @JoinColumn(name = "superseding_charge_id")
  var supersedingCharge: ChargeEntity?,
  @Column
  val terrorRelated: Boolean?,
  @Column
  val createdAt: ZonedDateTime = ZonedDateTime.now(),
  @Column
  val createdByUsername: String,
  @Type(value = JsonType::class)
  @Column(columnDefinition = "jsonb")
  var legacyData: JsonNode? = null,
  @ManyToMany(mappedBy = "charges")
  val courtAppearances: MutableSet<CourtAppearanceEntity>,
) {
  @OneToMany(mappedBy = "charge")
  var sentences: MutableList<SentenceEntity> = mutableListOf()

  fun hasNoActiveCourtAppearances(): Boolean = courtAppearances.none { it.statusId == EntityStatus.ACTIVE }

  fun hasTwoOrMoreActiveCourtAppearance(courtAppearance: CourtAppearanceEntity): Boolean = (courtAppearances + courtAppearance).count { it.statusId == EntityStatus.ACTIVE } >= 2

  fun getActiveSentence(): SentenceEntity? {
    return sentences.firstOrNull { it.statusId == EntityStatus.ACTIVE }
  }
  fun isSame(other: ChargeEntity): Boolean {
    return this.offenceCode == other.offenceCode &&
      ((this.offenceStartDate == null && other.offenceStartDate == null) || this.offenceStartDate?.isEqual(other.offenceStartDate) == true) &&
      ((this.offenceEndDate == null && other.offenceEndDate == null) || this.offenceEndDate?.isEqual(other.offenceEndDate) == true) &&
      this.statusId == other.statusId &&
      this.chargeOutcome == other.chargeOutcome &&
      this.terrorRelated == other.terrorRelated &&
      this.legacyData == other.legacyData
  }

  fun copyFrom(charge: LegacyCreateCharge, chargeOutcome: ChargeOutcomeEntity?, createdByUsername: String, legacyData: JsonNode): ChargeEntity {
    return ChargeEntity(
      0, lifetimeChargeUuid, UUID.randomUUID(), charge.offenceCode, charge.offenceStartDate, charge.offenceEndDate,
      EntityStatus.ACTIVE, chargeOutcome, this, terrorRelated,
      ZonedDateTime.now(), createdByUsername, legacyData, courtAppearances.toMutableSet(),
    )
  }

  fun copyFrom(charge: LegacyUpdateCharge, chargeOutcome: ChargeOutcomeEntity?, createdByUsername: String, legacyData: JsonNode): ChargeEntity {
    return ChargeEntity(
      0, lifetimeChargeUuid, UUID.randomUUID(), offenceCode, charge.offenceStartDate, charge.offenceEndDate,
      EntityStatus.ACTIVE, chargeOutcome, this, terrorRelated,
      ZonedDateTime.now(), createdByUsername, legacyData, courtAppearances.toMutableSet(),
    )
  }

  fun copyFrom(charge: CreateCharge, chargeOutcome: ChargeOutcomeEntity?, legacyData: JsonNode?, createdByUsername: String): ChargeEntity {
    return ChargeEntity(
      0, lifetimeChargeUuid, UUID.randomUUID(), charge.offenceCode, charge.offenceStartDate, charge.offenceEndDate,
      EntityStatus.ACTIVE, chargeOutcome, this, charge.terrorRelated, ZonedDateTime.now(), createdByUsername, legacyData, courtAppearances.toMutableSet(),
    )
  }

  fun copyFrom(chargeOutcome: ChargeOutcomeEntity?, createdByUsername: String): ChargeEntity {
    val charge = ChargeEntity(
      0, lifetimeChargeUuid, UUID.randomUUID(), offenceCode, offenceStartDate, offenceEndDate,
      EntityStatus.ACTIVE, chargeOutcome, this, terrorRelated, ZonedDateTime.now(), createdByUsername, legacyData, courtAppearances.toMutableSet(),
    )
    charge.sentences = sentences.toMutableList()
    return charge
  }

  fun copyFrom(migrationCreateCharge: MigrationCreateCharge, chargeOutcome: ChargeOutcomeEntity?, legacyData: JsonNode, createdByUsername: String): ChargeEntity {
    return ChargeEntity(
      0, lifetimeChargeUuid, UUID.randomUUID(), migrationCreateCharge.offenceCode, migrationCreateCharge.offenceStartDate, migrationCreateCharge.offenceEndDate,
      EntityStatus.ACTIVE, chargeOutcome, this, null,
      ZonedDateTime.now(), createdByUsername, legacyData, mutableSetOf(),
    )
  }

  fun copyFrom(charge: LegacyUpdateWholeCharge, createdByUsername: String): ChargeEntity {
    val charge = ChargeEntity(
      0, lifetimeChargeUuid, UUID.randomUUID(), charge.offenceCode, offenceStartDate, offenceEndDate,
      EntityStatus.ACTIVE, chargeOutcome, this, terrorRelated, ZonedDateTime.now(), createdByUsername, legacyData, courtAppearances.toMutableSet(),
    )
    charge.sentences = sentences.toMutableList()
    return charge
  }

  companion object {
    fun from(charge: CreateCharge, chargeOutcome: ChargeOutcomeEntity?, legacyData: JsonNode?, createdByUsername: String): ChargeEntity {
      return ChargeEntity(lifetimeChargeUuid = charge.lifetimeChargeUuid, chargeUuid = charge.chargeUuid, offenceCode = charge.offenceCode, offenceStartDate = charge.offenceStartDate, offenceEndDate = charge.offenceEndDate, statusId = EntityStatus.ACTIVE, chargeOutcome = chargeOutcome, supersedingCharge = null, terrorRelated = charge.terrorRelated, legacyData = legacyData, courtAppearances = mutableSetOf(), createdByUsername = createdByUsername)
    }

    fun from(charge: LegacyCreateCharge, chargeOutcome: ChargeOutcomeEntity?, legacyData: JsonNode, createdByUsername: String): ChargeEntity {
      return ChargeEntity(lifetimeChargeUuid = UUID.randomUUID(), chargeUuid = UUID.randomUUID(), offenceCode = charge.offenceCode, offenceStartDate = charge.offenceStartDate, offenceEndDate = charge.offenceEndDate, statusId = EntityStatus.ACTIVE, chargeOutcome = chargeOutcome, supersedingCharge = null, terrorRelated = null, legacyData = legacyData, courtAppearances = mutableSetOf(), createdByUsername = createdByUsername)
    }

    fun from(migrationCreateCharge: MigrationCreateCharge, chargeOutcome: ChargeOutcomeEntity?, legacyData: JsonNode, createdByUsername: String): ChargeEntity {
      return ChargeEntity(lifetimeChargeUuid = UUID.randomUUID(), chargeUuid = UUID.randomUUID(), offenceCode = migrationCreateCharge.offenceCode, offenceStartDate = migrationCreateCharge.offenceStartDate, offenceEndDate = migrationCreateCharge.offenceEndDate, statusId = EntityStatus.ACTIVE, chargeOutcome = chargeOutcome, supersedingCharge = null, terrorRelated = null, legacyData = legacyData, courtAppearances = mutableSetOf(), createdByUsername = createdByUsername)
    }
  }
}
