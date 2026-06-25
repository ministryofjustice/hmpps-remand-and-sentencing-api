package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.linklatestappearance

data class LatestCourtAppearanceKey(
  val courtCaseId: Int,
  val latestAppearanceId: Int,
)
