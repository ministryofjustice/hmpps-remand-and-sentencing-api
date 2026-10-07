package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.projection

import java.math.BigDecimal
import java.time.LocalDate

class ImprisonmentInDefaultOfFine(
  val prisonerId: String,
  val courtCode: String,
  val courtAppearanceId: Int,
  val appearanceDate: LocalDate,
  val fineAmount: BigDecimal?,
  val offenceCode: String,
  val sentenceId: Int,
  val days: Int?,
  val weeks: Int?,
  val months: Int?,
  val years: Int?,
  val periodOrder: String?,
)
