package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import java.time.LocalDate
import java.util.UUID

data class CreateSentence(
  val sentenceUuid: UUID?,
  val chargeNumber: String,
  val periodLengths: List<CreatePeriodLength>,
  val sentenceServeType: String,
  val consecutiveToChargeNumber: String?,
  val sentenceTypeId: UUID,
  val convictionDate: LocalDate?,
)
