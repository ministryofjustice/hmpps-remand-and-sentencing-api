package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.FineAmountEntity
import java.math.BigDecimal

data class FineAmount(
  val fineAmount: BigDecimal,
) {
  companion object {
    fun from(fineAmountEntity: FineAmountEntity): FineAmount {
      return FineAmount(
        fineAmountEntity.fineAmount,
      )
    }
  }
}
