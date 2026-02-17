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
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.chargeoutcome.CreateChargeOutcome
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ReferenceEntityStatus
import java.util.UUID

@Entity
@Table(name = "charge_outcome")
@DynamicUpdate
class ChargeOutcomeEntity(
  @Id
  @Column
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Int = 0,
  @Column
  var outcomeName: String,
  var outcomeUuid: UUID,
  var nomisCode: String,
  var outcomeType: String,
  var displayOrder: Int,
  var dispositionCode: String,
  @Enumerated(EnumType.STRING)
  var status: ReferenceEntityStatus,
) {
  fun updateFrom(existingUuid: UUID, createChargeOutcome: CreateChargeOutcome) {
    outcomeName = createChargeOutcome.outcomeName
    outcomeUuid = createChargeOutcome.outcomeUuid ?: existingUuid
    nomisCode = createChargeOutcome.nomisCode
    outcomeType = createChargeOutcome.outcomeType
    displayOrder = createChargeOutcome.displayOrder
    dispositionCode = createChargeOutcome.dispositionCode
    status = createChargeOutcome.status
  }
  companion object {
    fun from(createChargeOutcome: CreateChargeOutcome): ChargeOutcomeEntity = ChargeOutcomeEntity(
      0,
      createChargeOutcome.outcomeName,
      createChargeOutcome.outcomeUuid ?: UUID.randomUUID(),
      createChargeOutcome.nomisCode,
      createChargeOutcome.outcomeType,
      createChargeOutcome.displayOrder,
      createChargeOutcome.dispositionCode,
      createChargeOutcome.status,
    )
  }
}
