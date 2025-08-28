package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.merge

data class MergePerson(
  val removedPrisonerNumber: String,
  val casesCreated: List<MergeCreateCourtCase>,
  val casesDeactivated: List<DeactivatedCourtCase>,
  val sentencesDeactivated: List<DeactivatedSentence>,
)
