package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import java.util.UUID

data class HmctsHearingAutopopulateEligibility(
  val prisonerNumber: String,
  val hearingId: UUID,
  val cases: List<ExistingCaseReferenceAndId>,
  val features: List<HmctsAutopopulateFeature>,
  val hasBeenCompleted: Boolean = false,
  val hasWarrantAndPcr: Boolean = true,
)

data class HmctsAutopopulateFeature(
  val type: HmctsAutopopulateFeatureType,
  val enabled: Boolean,
)

data class ExistingCaseReferenceAndId(
  val caseReference: String,
  val caseUniqueIdentifier: String,
)

enum class HmctsAutopopulateFeatureType {
  MULTIPLE_CASE_REFERENCES,
  REMAND_WARRANT,
  SENTENCING_WARRANT,
  NEW_REMAND_APPEARANCE_ON_EXISTING_CASE,
  NEW_SENTENCING_APPEARANCE_ON_EXISTING_CASE,
}
