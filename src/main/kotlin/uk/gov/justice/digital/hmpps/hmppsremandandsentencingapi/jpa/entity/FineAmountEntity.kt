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
  @Column
  var fineAmount: BigDecimal,
  @OneToOne
  @JoinColumn(name = "sentence_id")
  var sentenceEntity: SentenceEntity?,
) {
  fun isSame(other: FineAmountEntity?): Boolean {
    return other?.fineAmount?.compareTo(fineAmount) == 0
  }

  companion object {
    fun from(fineAmount: CreateFineAmount): FineAmountEntity {
      return FineAmountEntity(fineAmount = fineAmount.fineAmount, sentenceEntity = null)
    }
  }
}