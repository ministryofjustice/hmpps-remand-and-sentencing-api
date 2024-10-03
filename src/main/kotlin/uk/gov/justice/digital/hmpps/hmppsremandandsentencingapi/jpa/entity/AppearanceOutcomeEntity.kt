package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "appearance_outcome")
data class AppearanceOutcomeEntity(
  @Id
  @Column
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Int = 0,
  val outcomeName: String,
  val outcomeUuid: UUID,
  val nomisCode: String,
  val outcomeType: String,
  val displayOrder: Int,
  val relatedChargeOutcomeUuid: UUID,
  val isSubList: Boolean,
)
