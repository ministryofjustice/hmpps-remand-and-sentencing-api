package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.RecallEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.RecallType
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

data class Recall(
  val recallUuid: UUID,
  val prisonerId: String,
  val revocationDate: LocalDate,
  val returnToCustodyDate: LocalDate,
  val recallType: RecallType,
  val createdAt: ZonedDateTime,
  val createdByUsername: String,
  val createdByPrison: String,
) {
  companion object {
    fun from(recall: RecallEntity): Recall =
      Recall(
        recallUuid = recall.recallUuid,
        prisonerId = recall.prisonerId,
        revocationDate = recall.revocationDate,
        returnToCustodyDate = recall.returnToCustodyDate,
        recallType = recall.recallType.code,
        createdByUsername = recall.createdByUsername,
        createdAt = recall.createdAt,
        createdByPrison = recall.createdByPrison,
      )
  }
}
