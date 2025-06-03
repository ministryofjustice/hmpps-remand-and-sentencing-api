package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.paged

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.projection.CourtCaseRow
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.ChargeLegacyData
import java.time.LocalDate

data class PagedCharge(
  val offenceCode: String,
  val offenceStartDate: LocalDate,
  val offenceEndDate: LocalDate?,
  val outcome: PagedChargeOutcome?,
  val legacyData: ChargeLegacyData?,
  val sentence: PagedSentence?,
) {
  companion object {
    fun from(chargeRows: List<CourtCaseRow>): PagedCharge {
      val charge = chargeRows.first()
      val sentenceRows = chargeRows.filter { it.sentenceId != null && it.sentenceStatus != EntityStatus.DELETED }.groupBy { it.sentenceId!! }.values.firstOrNull()
      return PagedCharge(
        charge.chargeOffenceCode!!,
        charge.chargeOffenceStartDate!!,
        charge.chargeOffenceEndDate,
        charge.chargeOutcomeUuid?.let { PagedChargeOutcome(it, charge.chargeOutcomeName!!) },
        charge.chargeLegacyData,
        sentenceRows?.let { PagedSentence.from(it) },
      )
    }
  }
}
