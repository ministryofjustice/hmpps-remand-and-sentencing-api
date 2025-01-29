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
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCreateFine
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateFine
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
  fun isSame(other: FineAmountEntity?): Boolean = other?.fineAmount?.compareTo(fineAmount) == 0

  companion object {
    fun from(fineAmount: CreateFineAmount): FineAmountEntity = FineAmountEntity(fineAmount = fineAmount.fineAmount, sentenceEntity = null)

    fun from(legacyCreateFine: LegacyCreateFine): FineAmountEntity = FineAmountEntity(fineAmount = legacyCreateFine.fineAmount, sentenceEntity = null)

    fun from(migrationCreateFine: MigrationCreateFine): FineAmountEntity = FineAmountEntity(fineAmount = migrationCreateFine.fineAmount, sentenceEntity = null)
  }
}
