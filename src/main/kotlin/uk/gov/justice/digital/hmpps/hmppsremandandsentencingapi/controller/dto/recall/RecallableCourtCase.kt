package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.recall

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import java.time.LocalDate

data class RecallableCourtCase(
  val courtCaseUuid: String,
  val reference: String,
  val courtCode: String,
  val status: EntityStatus,
  val isSentenced: Boolean,
  val sentences: List<RecallableCourtCaseSentence>,
  val date: LocalDate,
  val firstDayInCustody: LocalDate?,
)
