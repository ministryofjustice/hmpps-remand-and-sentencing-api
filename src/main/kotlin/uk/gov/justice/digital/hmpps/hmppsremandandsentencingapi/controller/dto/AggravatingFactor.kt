package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.AggravatingFactorEntity

data class AggravatingFactor(
  val code: String,
  val title: String,
  val description: String?,
  val displayOrder: Int,
) {
  companion object {
    fun from(aggravatingFactorEntity: AggravatingFactorEntity): AggravatingFactor = AggravatingFactor(
      aggravatingFactorEntity.code,
      aggravatingFactorEntity.title,
      aggravatingFactorEntity.description,
      aggravatingFactorEntity.displayOrder,
    )
  }
}