package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.RecallEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.RecallType
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

data class Recall(
  val recallUniqueIdentifier: UUID,
  val prisonerId: String,
  val recallDate: LocalDate,
  val returnToCustodyDate: LocalDate,
  val recallType: RecallType,
  val createdAt: ZonedDateTime,
  val createdByUsername: String,
) {
  companion object {
    fun transform(recall: RecallEntity): Recall =
      Recall(
        recallUniqueIdentifier = recall.recallUniqueIdentifier,
        prisonerId = recall.prisonerId,
        recallDate = recall.recallDate,
        returnToCustodyDate = recall.returnToCustodyDate,
        recallType = recall.recallType,
        createdByUsername = recall.createdByUsername,
        createdAt = recall.createdAt,
      )
  }
}
