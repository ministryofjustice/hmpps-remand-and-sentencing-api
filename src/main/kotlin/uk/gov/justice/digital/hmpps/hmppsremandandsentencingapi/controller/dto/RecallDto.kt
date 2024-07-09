package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.RecallType
import java.time.LocalDate

data class CreateRecall(
  val prisonerId: String,
  val recallDate: LocalDate,
  val returnToCustodyDate: LocalDate,
  val recallType: RecallType,
  val createdByUsername: String,
)

data class UpdateRecall(
  val recallDate: LocalDate,
  val returnToCustodyDate: LocalDate,
  val recallType: RecallType,
)
