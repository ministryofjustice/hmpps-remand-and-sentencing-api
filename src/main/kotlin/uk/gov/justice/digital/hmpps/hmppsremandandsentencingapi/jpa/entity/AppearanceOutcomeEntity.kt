package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.DynamicUpdate
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.appearanceoutcome.CreateAppearanceOutcome
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ReferenceEntityStatus
import java.util.UUID

@Entity
@Table(name = "appearance_outcome")
@DynamicUpdate
class AppearanceOutcomeEntity(
  @Id
  @Column
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Int = 0,
  var outcomeName: String,
  var outcomeUuid: UUID,
  var nomisCode: String,
  var outcomeType: String,
  var warrantType: String,
  var displayOrder: Int,
  var relatedChargeOutcomeUuid: UUID,
  var isSubList: Boolean,
  @Enumerated(EnumType.STRING)
  var status: ReferenceEntityStatus,
  var dispositionCode: String,
) {

  fun updateFrom(existingUuid: UUID, updateAppearanceOutcome: CreateAppearanceOutcome) {
    outcomeName = updateAppearanceOutcome.outcomeName
    outcomeUuid = updateAppearanceOutcome.outcomeUuid ?: existingUuid
    nomisCode = updateAppearanceOutcome.nomisCode
    outcomeType = updateAppearanceOutcome.outcomeType
    warrantType = updateAppearanceOutcome.warrantType
    displayOrder = updateAppearanceOutcome.displayOrder
    relatedChargeOutcomeUuid = updateAppearanceOutcome.relatedChargeOutcomeUuid
    isSubList = updateAppearanceOutcome.isSubList
    status = updateAppearanceOutcome.status
    dispositionCode = updateAppearanceOutcome.dispositionCode
  }
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
