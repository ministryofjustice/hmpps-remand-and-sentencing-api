package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.model

data class SentenceEligibility(
  val toreraEligibilityType: ToreraEligibilityType?,
  val sdsPlusEligibilityType: SDSPlusEligibilityType?,
)
