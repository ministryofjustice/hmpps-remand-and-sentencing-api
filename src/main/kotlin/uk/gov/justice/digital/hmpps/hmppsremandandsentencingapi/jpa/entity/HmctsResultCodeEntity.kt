package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.DynamicUpdate
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.hmctsresultcode.CreateHmctsResultCode
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ReferenceEntityStatus
import java.util.UUID

@Entity
@Table(name = "hmcts_result_code")
@DynamicUpdate
class HmctsResultCodeEntity(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Int = 0,
  var resultName: String,
  var resultUuid: UUID,
  var resultCode: String,
  @ManyToOne
  @JoinColumn(name = "charge_outcome_id")
  var chargeOutcome: ChargeOutcomeEntity,
  @Enumerated(EnumType.STRING)
  var status: ReferenceEntityStatus,

) {
  fun updateFrom(existingUuid: UUID, createHmctsResultCode: CreateHmctsResultCode, outcome: ChargeOutcomeEntity) {
    resultUuid = createHmctsResultCode.resultUuid ?: existingUuid
    resultName = createHmctsResultCode.resultName
    resultCode = createHmctsResultCode.resultCode
    chargeOutcome = outcome
    status = createHmctsResultCode.status
  }

  companion object {
    fun from(createHmctsResultCode: CreateHmctsResultCode, outcome: ChargeOutcomeEntity): HmctsResultCodeEntity = HmctsResultCodeEntity(
      0,
      createHmctsResultCode.resultName,
      createHmctsResultCode.resultUuid ?: UUID.randomUUID(),
      createHmctsResultCode.resultCode,
      outcome,
      createHmctsResultCode.status,
    )
  }
}
