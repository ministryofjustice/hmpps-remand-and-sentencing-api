package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.embeddable

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.io.Serializable

@Embeddable
class ChargeOutcomeSentenceTypeId(
  @Column(name = "charge_outcome_id")
  val chargeOutcomeId: Int,

  @Column(name = "sentence_type_id")
  val sentenceTypeId: Int,
) : Serializable {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as ChargeOutcomeSentenceTypeId

    return chargeOutcomeId == other.chargeOutcomeId && sentenceTypeId == other.sentenceTypeId
  }

  override fun hashCode(): Int = 31 * chargeOutcomeId.hashCode() + sentenceTypeId.hashCode()
}
