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
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.annotations.Type
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCharge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
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
  val offenceStartDate: LocalDate,
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
  val createdAt: ZonedDateTime = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS),
  @Type(value = JsonType::class)
  @Column(columnDefinition = "jsonb")
  var legacyData: JsonNode? = null,

) {
  @OneToMany(mappedBy = "charge")
  var sentences: MutableList<SentenceEntity> = mutableListOf()

  fun getActiveSentence(): SentenceEntity? {
    return sentences.firstOrNull { it.statusId == EntityStatus.ACTIVE }
  }
  fun isSame(other: ChargeEntity): Boolean {
    return this.chargeUuid == other.chargeUuid &&
      this.offenceCode == other.offenceCode &&
      this.offenceStartDate.isEqual(other.offenceStartDate) &&
      ((this.offenceEndDate == null && other.offenceEndDate == null) || this.offenceEndDate?.isEqual(other.offenceEndDate) == true) &&
      this.statusId == other.statusId &&
      this.chargeOutcome == other.chargeOutcome &&
      this.terrorRelated == other.terrorRelated &&
      this.legacyData == other.legacyData
  }

  companion object {
    fun from(charge: CreateCharge, chargeOutcome: ChargeOutcomeEntity?, legacyData: JsonNode?): ChargeEntity {
      return ChargeEntity(lifetimeChargeUuid = UUID.randomUUID(), chargeUuid = charge.chargeUuid ?: UUID.randomUUID(), offenceCode = charge.offenceCode, offenceStartDate = charge.offenceStartDate, offenceEndDate = charge.offenceEndDate, statusId = EntityStatus.ACTIVE, chargeOutcome = chargeOutcome, supersedingCharge = null, terrorRelated = charge.terrorRelated, legacyData = legacyData)
    }
  }
}
