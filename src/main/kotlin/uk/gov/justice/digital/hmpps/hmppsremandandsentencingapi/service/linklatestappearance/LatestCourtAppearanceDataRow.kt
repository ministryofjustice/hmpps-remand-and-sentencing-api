package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.linklatestappearance

data class LatestCourtAppearanceDataRow(
  val courtCaseId: Int,
  val latestAppearanceId: Int,
  val futureAppearanceId: Int,
  val nextCourtAppearanceId: Int?,
  val incorrectAppearanceId: Int?,
)
