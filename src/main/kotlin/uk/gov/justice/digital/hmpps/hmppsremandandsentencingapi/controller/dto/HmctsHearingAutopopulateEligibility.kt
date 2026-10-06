package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

data class HmctsHearingAutopopulateEligibility(
  val features: List<HmctsAutopopulateFeature>,
  val existingCaseIdentifier: String? = null,
  val hasBeenCompleted: Boolean = false,
  val hasWarrantAndPcr: Boolean = true,
)

data class HmctsAutopopulateFeature(
  val type: HmctsAutopopulateFeatureType,
  val enabled: Boolean,
)

enum class HmctsAutopopulateFeatureType {
  MULTIPLE_CASE_REFERENCES,
  REMAND_WARRANT,
  SENTENCING_WARRANT,
  NEW_REMAND_APPEARANCE_ON_EXISTING_CASE,
  NEW_SENTENCING_APPEARANCE_ON_EXISTING_CASE,
}
