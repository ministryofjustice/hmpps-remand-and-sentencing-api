package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity

import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.ManyToOne
import jakarta.persistence.MapsId
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.embeddable.ChargeOutcomeSentenceTypeId
import java.io.Serializable
import java.time.LocalDate

@Entity
@Table(name = "charge_outcome_sentence_type")
class ChargeOutcomeSentenceTypeEntity(
  @EmbeddedId
  val id: ChargeOutcomeSentenceTypeId,
  @ManyToOne
  @MapsId("chargeOutcomeId")
  val chargeOutcome: ChargeOutcomeEntity,
  @ManyToOne
  @MapsId("sentenceTypeId")
  val sentenceType: SentenceTypeEntity,

  val maxDateExclusive: LocalDate?,
) : Serializable {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is ChargeOutcomeSentenceTypeEntity) return false

    if (chargeOutcome != other.chargeOutcome) return false
    if (sentenceType != other.sentenceType) return false

    return true
  }

  override fun hashCode(): Int {
    var result = chargeOutcome.hashCode()
    result = 31 * result + sentenceType.hashCode()
    return result
  }
}
