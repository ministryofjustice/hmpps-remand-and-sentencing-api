package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.RecallType
import java.time.LocalDate

data class CreateRecall(
  val prisonerId: String,
  val revocationDate: LocalDate? = null,
  val returnToCustodyDate: LocalDate? = null,
  val recallTypeCode: RecallType,
  val createdByUsername: String,
  val createdByPrison: String,
)
