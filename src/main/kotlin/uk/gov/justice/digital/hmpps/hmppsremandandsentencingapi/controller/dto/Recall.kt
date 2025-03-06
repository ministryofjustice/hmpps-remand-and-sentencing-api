package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.RecallEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.RecallSentenceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.RecallType
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

data class Recall(
  val recallUuid: UUID,
  val prisonerId: String,
  val revocationDate: LocalDate?,
  val returnToCustodyDate: LocalDate?,
  val recallType: RecallType,
  val createdAt: ZonedDateTime,
  val createdByUsername: String,
  val createdByPrison: String,
  val sentences: List<Sentence>? = emptyList(),
  val courtCaseIds: List<String>? = emptyList(),
) {
  companion object {
    fun from(recall: RecallEntity, sentences: List<RecallSentenceEntity>): Recall = Recall(
      recallUuid = recall.recallUuid,
      prisonerId = recall.prisonerId,
      revocationDate = recall.revocationDate,
      returnToCustodyDate = recall.returnToCustodyDate,
      recallType = recall.recallType.code,
      createdByUsername = recall.createdByUsername,
      createdAt = recall.createdAt,
      createdByPrison = recall.createdByPrison,
      sentences = sentences.map { Sentence.from(it.sentence) },
      courtCaseIds = sentences.flatMap { it.sentence.charge.appearanceCharges.map { ac -> ac.courtAppearance.courtCase.caseUniqueIdentifier } },
    )
  }
}
