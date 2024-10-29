package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateFineAmount
import java.math.BigDecimal

@Entity
@Table(name = "fine_amount")
class FineAmountEntity(
  @Id
  @Column
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Int = 0,
  @OneToOne
  @JoinColumn(name = "sentence_id")
  var sentence: SentenceEntity,
  @Column
  var fineAmount: BigDecimal,
) {
  fun isSame(other: FineAmountEntity?): Boolean {
    return sentence == other?.sentence &&
      fineAmount == other.fineAmount
  }

  companion object {
    fun from(fineAmount: CreateFineAmount): FineAmountEntity {
      return FineAmountEntity(sentence = SentenceEntity.from(fineAmount.sentence), fineAmount = fineAmount.fineAmount)
    }
  }
}
