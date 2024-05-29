package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import java.util.UUID

data class CreateSentence(
  val sentenceUuid: UUID?,
  val chargeNumber: String,
  val custodialPeriodLength: CreatePeriodLength,
  val extendedLicensePeriodLength: CreatePeriodLength?,
  val sentenceServeType: String,
  val consecutiveToChargeNumber: String?,
  val sentenceType: String,
)
