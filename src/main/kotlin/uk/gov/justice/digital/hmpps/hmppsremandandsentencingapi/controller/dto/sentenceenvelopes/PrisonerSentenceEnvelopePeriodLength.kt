package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.sentenceenvelopes

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.PeriodLengthType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.projection.ViewSentenceRow
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.PeriodLengthLegacyData
import java.util.UUID

data class PrisonerSentenceEnvelopePeriodLength(
  val years: Int?,
  val months: Int?,
  val weeks: Int?,
  val days: Int?,
  val periodOrder: List<String>,
  val periodLengthType: PeriodLengthType,
  val legacyData: PeriodLengthLegacyData?,
  val periodLengthUuid: UUID,
) {
  companion object {
    fun from(viewSentenceRowEntry: Map.Entry<UUID, List<ViewSentenceRow>>): PrisonerSentenceEnvelopePeriodLength {
      val (periodLengthUuid, viewSentenceRows) = viewSentenceRowEntry
      val firstRow = viewSentenceRows.first()
      return PrisonerSentenceEnvelopePeriodLength(
        firstRow.years,
        firstRow.months,
        firstRow.weeks,
        firstRow.days,
        firstRow.periodOrder!!.split(","),
        firstRow.periodLengthType!!,
        firstRow.periodLengthLegacyData,
        periodLengthUuid,
      )
    }
  }
}
