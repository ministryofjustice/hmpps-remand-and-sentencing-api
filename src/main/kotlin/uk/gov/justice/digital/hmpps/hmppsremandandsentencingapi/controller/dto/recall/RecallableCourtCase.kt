package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.recall

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import java.time.LocalDate

data class RecallableCourtCase(
  val caseId: String,
  val reference: String,
  val court: String,
  val date: LocalDate,
  val status: EntityStatus,
  val isSentenced: Boolean,
  val sentences: List<RecallableSentence>,
)
