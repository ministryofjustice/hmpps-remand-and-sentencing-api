package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.RecallEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.RecallType
import java.time.LocalDate
import java.util.UUID

data class LegacyRecall(
  val recallUuid: UUID,
  val prisonerId: String,
  val returnToCustodyDate: LocalDate?,
  val revocationDate: LocalDate?,
  val sentenceIds: List<UUID>,
  val recallType: RecallType,
  val recallBy: String,
) {
  companion object {
    fun from(recallEntity: RecallEntity): LegacyRecall = LegacyRecall(
      recallEntity.recallUuid,
      recallEntity.prisonerId,
      if (recallEntity.inPrisonOnRevocationDate == true) recallEntity.revocationDate else recallEntity.returnToCustodyDate,
      recallEntity.revocationDate,
      recallEntity.recallSentences.map { it.sentence }.filter { it.statusId != EntityStatus.DELETED }.map { it.sentenceUuid },
      recallType = recallEntity.recallType.code,
      recallBy = recallEntity.updatedBy ?: recallEntity.createdByUsername,
    )
  }
}
