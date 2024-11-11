package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.FineAmountEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceEntity
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class FineAmount(
  val fineAmount: BigDecimal
) {
  companion object {
    fun from(fineAmountEntity: FineAmountEntity): FineAmount {
      return FineAmount(
        fineAmountEntity.fineAmount
      )
    }
  }
}