package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.projection

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.NextCourtAppearanceEntity

data class FirstNextCourtAppearance(
  val nextCourtAppearanceEntity: NextCourtAppearanceEntity,
  val caseReference: String?,
)
