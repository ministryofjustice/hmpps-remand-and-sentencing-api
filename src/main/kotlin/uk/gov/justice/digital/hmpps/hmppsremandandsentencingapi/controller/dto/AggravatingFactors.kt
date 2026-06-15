package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

data class AggravatingFactors(
  val isDomesticViolenceRelated: Boolean? = null,
  val isForeignPowerRelated: Boolean? = null,
  val isTerrorRelated: Boolean? = null,
)
