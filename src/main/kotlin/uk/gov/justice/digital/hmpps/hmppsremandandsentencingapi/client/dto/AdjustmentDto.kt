package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto

import java.time.LocalDate

data class AdjustmentDto(
  val id: String?,
  val person: String,
  val adjustmentType: String,
  val toDate: LocalDate?,
  val fromDate: LocalDate?,
  val days: Int?,
  val recallId: String?,
  val unlawfullyAtLarge: UnlawfullyAtLargeDto?,
)
