package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.sentenceenvelopes

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.PeriodLengthType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.PeriodLengthLegacyData
import java.util.UUID

data class PrisonerSentenceEnvelopePeriodLength(
  val years: Int?,
  val months: Int?,
  val weeks: Int?,
  val days: Int?,
  val periodOrder: String,
  val periodLengthType: PeriodLengthType,
  val legacyData: PeriodLengthLegacyData?,
  val periodLengthUuid: UUID,
)
