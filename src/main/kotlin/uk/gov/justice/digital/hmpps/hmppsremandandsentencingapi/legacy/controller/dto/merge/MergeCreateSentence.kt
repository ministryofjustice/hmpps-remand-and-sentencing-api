package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.merge

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.SentenceLegacyData
import java.time.LocalDate

data class MergeCreateSentence(
  val sentenceId: MergeSentenceId,
  val fine: MergeCreateFine?,
  val active: Boolean,
  var legacyData: SentenceLegacyData,
  val consecutiveToSentenceId: MergeSentenceId?,
  val periodLengths: List<MergeCreatePeriodLength>,
  val returnToCustodyDate: LocalDate? = null,
)
