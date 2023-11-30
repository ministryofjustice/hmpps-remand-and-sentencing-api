package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity

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
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "charge")
data class ChargeEntity(
  @Id
  @Column
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Int = 0,
  @Column
  val lifetimeChargeUuid: UUID,
  @Column
  val chargeUuid: UUID,
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
  val chargeOutcome: ChargeOutcomeEntity,
  @OneToOne
  @JoinColumn(name = "superseding_charge_id")
  val supersedingCharge: ChargeEntity?,
  @ManyToMany(mappedBy = "charges")
  val appearances: Set<CourtAppearanceEntity>,
)
