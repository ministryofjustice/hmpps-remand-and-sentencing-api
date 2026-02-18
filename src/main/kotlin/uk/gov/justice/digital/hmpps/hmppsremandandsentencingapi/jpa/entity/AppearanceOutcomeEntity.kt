package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.appearanceoutcome.CreateAppearanceOutcome
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ReferenceEntityStatus
import java.util.UUID

@Entity
@Table(name = "appearance_outcome")
class AppearanceOutcomeEntity(
  @Id
  @Column
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Int = 0,
  val outcomeName: String,
  val outcomeUuid: UUID,
  val nomisCode: String,
  val outcomeType: String,
  val warrantType: String,
  val displayOrder: Int,
  val relatedChargeOutcomeUuid: UUID,
  val isSubList: Boolean,
  @Enumerated(EnumType.STRING)
  val status: ReferenceEntityStatus,
  val dispositionCode: String,
) {
  companion object {
    fun from(createAppearanceOutcome: CreateAppearanceOutcome): AppearanceOutcomeEntity = AppearanceOutcomeEntity(
      0,
      createAppearanceOutcome.outcomeName,
      createAppearanceOutcome.outcomeUuid ?: UUID.randomUUID(),
      createAppearanceOutcome.nomisCode,
      createAppearanceOutcome.outcomeType,
      createAppearanceOutcome.warrantType,
      createAppearanceOutcome.displayOrder,
      createAppearanceOutcome.relatedChargeOutcomeUuid,
      createAppearanceOutcome.isSubList,
      createAppearanceOutcome.status,
      createAppearanceOutcome.dispositionCode,
    )
  }
}
