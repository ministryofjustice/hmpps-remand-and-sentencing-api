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
import org.hibernate.annotations.DynamicUpdate
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCharge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ChargeEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.CourtAppearanceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.ChargeLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCreateCharge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyLinkChargeToCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyUpdateCharge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyUpdateWholeCharge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateCharge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.booking.BookingCreateCharge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.merge.MergeCreateCharge
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

@DynamicUpdate
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
  @Enumerated(EnumType.STRING)
  var statusId: ChargeEntityStatus,
  @ManyToOne
  @JoinColumn(name = "charge_outcome_id")
  var chargeOutcome: ChargeOutcomeEntity?,
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "superseding_charge_id")
  var supersedingCharge: ChargeEntity?,
  var terrorRelated: Boolean?,
  var foreignPowerRelated: Boolean?,
  var domesticViolenceRelated: Boolean?,
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
  var mergedFromDate: LocalDate? = null,
) {
  @OneToMany(mappedBy = "charge")
  var sentences: MutableSet<SentenceEntity> = mutableSetOf()

  fun hasNoLiveCourtAppearances(): Boolean = appearanceCharges.none { it.appearance!!.statusId != CourtAppearanceEntityStatus.DELETED }

  fun hasTwoOrMoreLiveCourtAppearance(courtAppearance: CourtAppearanceEntity): Boolean = (appearanceCharges.map { it.appearance!! } + courtAppearance).toSet().count { it.statusId != CourtAppearanceEntityStatus.DELETED } >= 2

  fun getLiveSentence(): SentenceEntity? = sentences.firstOrNull { it.statusId != SentenceEntityStatus.DELETED }

  fun hasSentence(): Boolean = sentences.any { it.statusId != SentenceEntityStatus.DELETED }
  fun isSame(other: ChargeEntity, otherHasSentence: Boolean): Boolean = this.offenceCode == other.offenceCode &&
    ((this.offenceStartDate == null && other.offenceStartDate == null) || (other.offenceStartDate != null && this.offenceStartDate?.isEqual(other.offenceStartDate) == true)) &&
    ((this.offenceEndDate == null && other.offenceEndDate == null) || (other.offenceEndDate != null && this.offenceEndDate?.isEqual(other.offenceEndDate) == true)) &&
    this.statusId == other.statusId &&
    this.chargeOutcome == other.chargeOutcome &&
    this.terrorRelated == other.terrorRelated &&
    this.foreignPowerRelated == other.foreignPowerRelated &&
    this.domesticViolenceRelated == other.domesticViolenceRelated &&
    ((this.legacyData == null && other.legacyData == null) || legacyData?.isSame(other.legacyData) == true) &&
    this.mergedFromCourtCase == other.mergedFromCourtCase &&
    this.mergedFromDate == other.mergedFromDate &&
    otherHasSentence == hasSentence()

  fun copyFrom(charge: LegacyCreateCharge, chargeOutcome: ChargeOutcomeEntity?, createdBy: String): ChargeEntity = ChargeEntity(
    0, chargeUuid, charge.offenceCode, charge.offenceStartDate, charge.offenceEndDate,
    ChargeEntityStatus.ACTIVE, chargeOutcome, this, terrorRelated, foreignPowerRelated, domesticViolenceRelated,
    createdAt, this.createdBy, createdPrison, ZonedDateTime.now(), createdBy, updatedPrison ?: createdPrison, charge.legacyData,
    appearanceCharges.toMutableSet(),
    mergedFromCourtCase,
    mergedFromDate = mergedFromDate,
  )

  fun copyFrom(charge: LegacyUpdateCharge, chargeOutcome: ChargeOutcomeEntity?, createdBy: String): ChargeEntity = ChargeEntity(
    0, chargeUuid, offenceCode, charge.offenceStartDate, charge.offenceEndDate,
    ChargeEntityStatus.ACTIVE, chargeOutcome, this, terrorRelated, foreignPowerRelated, domesticViolenceRelated,
    createdAt, this.createdBy, createdPrison, ZonedDateTime.now(), createdBy, updatedPrison ?: createdPrison, charge.legacyData,
    appearanceCharges.toMutableSet(),
    mergedFromCourtCase,
    mergedFromDate = mergedFromDate,
  )

  fun copyFrom(linkChargeToCase: LegacyLinkChargeToCase, mergedFromCase: CourtCaseEntity, createdBy: String): ChargeEntity = ChargeEntity(
    0, chargeUuid, offenceCode, offenceStartDate, offenceEndDate, ChargeEntityStatus.ACTIVE, chargeOutcome, this, terrorRelated,
    foreignPowerRelated, domesticViolenceRelated, createdAt, this.createdBy, createdPrison,
    ZonedDateTime.now(), createdBy, updatedPrison ?: createdPrison, legacyData, mutableSetOf(),
    mergedFromCase, linkChargeToCase.linkedDate,
  )

  fun copyFrom(charge: CreateCharge, chargeOutcome: ChargeOutcomeEntity?, createdBy: String): ChargeEntity = ChargeEntity(
    0, chargeUuid, charge.offenceCode, charge.offenceStartDate, charge.offenceEndDate,
    ChargeEntityStatus.ACTIVE, chargeOutcome, this, charge.terrorRelated, charge.foreignPowerRelated, charge.domesticViolenceRelated,
    createdAt, this.createdBy, charge.prisonId, ZonedDateTime.now(), createdBy, charge.prisonId,
    charge.legacyData, appearanceCharges.toMutableSet(),
    mergedFromCourtCase,
    mergedFromDate = mergedFromDate,
  )

  fun copyFrom(chargeOutcome: ChargeOutcomeEntity?, createdBy: String): ChargeEntity {
    val charge = ChargeEntity(
      0, chargeUuid, offenceCode, offenceStartDate, offenceEndDate,
      ChargeEntityStatus.ACTIVE, chargeOutcome, this, terrorRelated, foreignPowerRelated, domesticViolenceRelated,
      createdAt, this.createdBy, createdPrison, ZonedDateTime.now(), createdBy, updatedPrison ?: createdPrison,
      legacyData, appearanceCharges.toMutableSet(),
      mergedFromCourtCase,
      mergedFromDate = mergedFromDate,
    )
    charge.sentences = sentences.toMutableSet()
    return charge
  }

  fun copyFrom(migrationCreateCharge: MigrationCreateCharge, chargeOutcome: ChargeOutcomeEntity?, createdBy: String): ChargeEntity = ChargeEntity(
    0, chargeUuid, migrationCreateCharge.offenceCode, migrationCreateCharge.offenceStartDate, migrationCreateCharge.offenceEndDate,
    ChargeEntityStatus.ACTIVE, chargeOutcome, this, terrorRelated, foreignPowerRelated, domesticViolenceRelated,
    createdAt, this.createdBy, null, ZonedDateTime.now(), createdBy, updatedPrison ?: createdPrison, migrationCreateCharge.legacyData, mutableSetOf(), null,
    mergedFromDate = migrationCreateCharge.mergedFromDate,
  )

  fun copyFrom(mergeCreateCharge: MergeCreateCharge, chargeOutcome: ChargeOutcomeEntity?, createdBy: String): ChargeEntity = ChargeEntity(
    0, chargeUuid, mergeCreateCharge.offenceCode, mergeCreateCharge.offenceStartDate, mergeCreateCharge.offenceEndDate,
    ChargeEntityStatus.ACTIVE, chargeOutcome, this, terrorRelated, foreignPowerRelated, domesticViolenceRelated,
    createdAt, this.createdBy, null, ZonedDateTime.now(), createdBy, updatedPrison ?: createdPrison, mergeCreateCharge.legacyData, mutableSetOf(), null,
    mergedFromDate = mergeCreateCharge.mergedFromDate,
  )

  fun copyFrom(bookingCreateCharge: BookingCreateCharge, chargeOutcome: ChargeOutcomeEntity?, createdBy: String): ChargeEntity = ChargeEntity(
    0, chargeUuid, bookingCreateCharge.offenceCode, bookingCreateCharge.offenceStartDate, bookingCreateCharge.offenceEndDate,
    ChargeEntityStatus.DUPLICATE, chargeOutcome, this, terrorRelated, foreignPowerRelated, domesticViolenceRelated,
    createdAt, this.createdBy, null, ZonedDateTime.now(), createdBy, updatedPrison ?: createdPrison, bookingCreateCharge.legacyData, mutableSetOf(), null,
    mergedFromDate = bookingCreateCharge.mergedFromDate,
  )

  fun copyFrom(charge: LegacyUpdateWholeCharge, createdBy: String): ChargeEntity {
    val chargeEntity = ChargeEntity(
      0, chargeUuid, charge.offenceCode, offenceStartDate, offenceEndDate,
      ChargeEntityStatus.ACTIVE, chargeOutcome, this, terrorRelated, foreignPowerRelated, domesticViolenceRelated,
      createdAt, this.createdBy, null, ZonedDateTime.now(), createdBy, updatedPrison ?: createdPrison,
      legacyData, appearanceCharges.toMutableSet(),
      mergedFromCourtCase,
      mergedFromDate = mergedFromDate,
    )
    chargeEntity.sentences = sentences.toMutableSet()
    return chargeEntity
  }

  fun copyFromReplacedCharge(replacedCharge: ChargeEntity): ChargeEntity {
    val currentDate = ZonedDateTime.now()
    val charge = ChargeEntity(
      0, UUID.randomUUID(), offenceCode, offenceStartDate, offenceEndDate, ChargeEntityStatus.ACTIVE, chargeOutcome, replacedCharge,
      terrorRelated, foreignPowerRelated, domesticViolenceRelated,
      currentDate, createdBy, createdPrison, currentDate, null, null, legacyData, appearanceCharges.toMutableSet(),
      mergedFromCourtCase, mergedFromDate,
    )
    charge.sentences = sentences.toMutableSet()
    return charge
  }

  fun copyFrom(updatedBy: String): ChargeEntity = ChargeEntity(
    0, chargeUuid, offenceCode, offenceStartDate, offenceEndDate, statusId, chargeOutcome, this,
    terrorRelated, foreignPowerRelated, domesticViolenceRelated, createdAt, createdBy, createdPrison, ZonedDateTime.now(), updatedBy, null, legacyData, mutableSetOf(), mergedFromCourtCase, mergedFromDate,
  )

  fun updateFrom(chargeEntity: ChargeEntity) {
    offenceCode = chargeEntity.offenceCode
    offenceStartDate = chargeEntity.offenceStartDate
    offenceEndDate = chargeEntity.offenceEndDate
    statusId = chargeEntity.statusId
    chargeOutcome = chargeEntity.chargeOutcome
    supersedingCharge = chargeEntity.supersedingCharge
    terrorRelated = chargeEntity.terrorRelated
    foreignPowerRelated = chargeEntity.foreignPowerRelated
    domesticViolenceRelated = chargeEntity.domesticViolenceRelated
    updatedAt = chargeEntity.updatedAt
    updatedBy = chargeEntity.updatedBy
    updatedPrison = chargeEntity.updatedPrison
    legacyData = chargeEntity.legacyData
    mergedFromCourtCase = chargeEntity.mergedFromCourtCase
    mergedFromDate = chargeEntity.mergedFromDate
  }

  fun updateFrom(chargeOutcome: ChargeOutcomeEntity?, username: String, prisonId: String) {
    this.chargeOutcome = chargeOutcome
    updatedAt = ZonedDateTime.now()
    updatedBy = username
    updatedPrison = prisonId
  }

  fun delete(deletedBy: String) {
    statusId = ChargeEntityStatus.DELETED
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
    fun from(
      charge: CreateCharge,
      chargeOutcome: ChargeOutcomeEntity?,
      createdBy: String,
      supersedingCharge: ChargeEntity?,
    ): ChargeEntity = ChargeEntity(
      chargeUuid = charge.chargeUuid,
      offenceCode = charge.offenceCode,
      offenceStartDate = charge.offenceStartDate,
      offenceEndDate = charge.offenceEndDate,
      statusId = ChargeEntityStatus.ACTIVE,
      chargeOutcome = chargeOutcome,
      supersedingCharge = supersedingCharge,
      terrorRelated = charge.terrorRelated,
      foreignPowerRelated = charge.foreignPowerRelated,
      domesticViolenceRelated = charge.domesticViolenceRelated,
      legacyData = charge.legacyData,
      appearanceCharges = mutableSetOf(),
      createdBy = createdBy,
      createdPrison = charge.prisonId,
    )

    fun from(charge: LegacyCreateCharge, chargeOutcome: ChargeOutcomeEntity?, createdBy: String): ChargeEntity = ChargeEntity(chargeUuid = UUID.randomUUID(), offenceCode = charge.offenceCode, offenceStartDate = charge.offenceStartDate, offenceEndDate = charge.offenceEndDate, statusId = ChargeEntityStatus.ACTIVE, chargeOutcome = chargeOutcome, supersedingCharge = null, terrorRelated = null, foreignPowerRelated = null, domesticViolenceRelated = null, legacyData = charge.legacyData, appearanceCharges = mutableSetOf(), createdBy = createdBy, createdPrison = null)

    fun from(migrationCreateCharge: MigrationCreateCharge, chargeOutcome: ChargeOutcomeEntity?, createdBy: String): ChargeEntity = ChargeEntity(
      0,
      UUID.randomUUID(),
      migrationCreateCharge.offenceCode,
      migrationCreateCharge.offenceStartDate,
      migrationCreateCharge.offenceEndDate,
      ChargeEntityStatus.ACTIVE,
      chargeOutcome,
      null,
      null,
      null,
      null,
      ZonedDateTime.now(),
      createdBy,
      null,
      ZonedDateTime.now(),
      null,
      null,

      migrationCreateCharge.legacyData,
      mutableSetOf(),
      null,
      migrationCreateCharge.mergedFromDate,
    )

    fun from(mergeCreateCharge: MergeCreateCharge, chargeOutcome: ChargeOutcomeEntity?, createdBy: String): ChargeEntity = ChargeEntity(
      0,
      UUID.randomUUID(),
      mergeCreateCharge.offenceCode,
      mergeCreateCharge.offenceStartDate,
      mergeCreateCharge.offenceEndDate,
      ChargeEntityStatus.ACTIVE,
      chargeOutcome,
      null,
      null,
      null,
      null,
      ZonedDateTime.now(),
      createdBy,
      null,
      ZonedDateTime.now(),
      null,
      null,

      mergeCreateCharge.legacyData,
      mutableSetOf(),
      null,
      mergeCreateCharge.mergedFromDate,
    )

    fun from(bookingCreateCharge: BookingCreateCharge, chargeOutcome: ChargeOutcomeEntity?, createdBy: String): ChargeEntity = ChargeEntity(
      0,
      UUID.randomUUID(),
      bookingCreateCharge.offenceCode,
      bookingCreateCharge.offenceStartDate,
      bookingCreateCharge.offenceEndDate,
      ChargeEntityStatus.DUPLICATE,
      chargeOutcome,
      null,
      null,
      null,
      null,
      ZonedDateTime.now(),
      createdBy,
      null,
      ZonedDateTime.now(),
      null,
      null,

      bookingCreateCharge.legacyData,
      mutableSetOf(),
      null,
      bookingCreateCharge.mergedFromDate,
    )
  }
}
