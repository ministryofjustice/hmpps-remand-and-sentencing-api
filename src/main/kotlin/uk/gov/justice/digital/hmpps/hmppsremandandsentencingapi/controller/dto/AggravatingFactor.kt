package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.AggravatingFactorEntity

class AggravatingFactor (
  val title: String,
  val status: String,
  val code: String,
  val order: Int,
){
  companion object {
    fun from(aggravatingFactorEntity: AggravatingFactorEntity): AggravatingFactor = AggravatingFactor(
      title = aggravatingFactorEntity.title,
      status = aggravatingFactorEntity.status.name,
      code = aggravatingFactorEntity.code,
      order = aggravatingFactorEntity.displayOrder
    )
  }
}