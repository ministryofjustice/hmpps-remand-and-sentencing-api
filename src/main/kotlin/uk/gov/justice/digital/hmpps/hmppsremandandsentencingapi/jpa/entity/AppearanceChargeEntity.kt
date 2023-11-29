package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "appearance_charge")
data class AppearanceChargeEntity(
  @Id
  @Column
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Int? = null,
  @ManyToOne
  @JoinColumn(name = "appearance_id")
  val courtAppearance: CourtAppearanceEntity,
  @ManyToOne
  @JoinColumn(name = "charge_id")
  val charge: ChargeEntity,
)
