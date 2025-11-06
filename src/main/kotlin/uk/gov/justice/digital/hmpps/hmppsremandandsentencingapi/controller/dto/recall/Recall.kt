package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.recall

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.AdjustmentDto
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.PeriodLength
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.EventSource
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.RecallEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.RecallSentenceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.CourtAppearanceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.RecallType
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

data class Recall(
  val recallUuid: UUID,
  val prisonerId: String,
  val revocationDate: LocalDate?,
  val returnToCustodyDate: LocalDate?,
  val inPrisonOnRevocationDate: Boolean?,
  val recallType: RecallType,
  val createdAt: ZonedDateTime,
  val createdByUsername: String,
  val createdByPrison: String?,
  val source: EventSource,
  val courtCases: List<RecallCourtCaseDetails> = emptyList(),
  val ual: RecallUALAdjustment? = null,
  val calculationRequestId: Int? = null,
) {
  @get:JsonProperty
  @get:Schema(description = "True if the recall was created manually (i.e. calculationRequestId is null)")
  val isManual: Boolean
    get() = calculationRequestId == null
  companion object {
    fun from(recall: RecallEntity, sentences: List<RecallSentenceEntity>, ualAdjustment: AdjustmentDto?): Recall = Recall(
      recallUuid = recall.recallUuid,
      prisonerId = recall.prisonerId,
      revocationDate = recall.revocationDate,
      returnToCustodyDate = recall.returnToCustodyDate,
      inPrisonOnRevocationDate = recall.inPrisonOnRevocationDate,
      recallType = recall.recallType.code,
      createdByUsername = recall.createdByUsername,
      createdAt = recall.createdAt,
      createdByPrison = recall.createdPrison,
      source = recall.source,
      courtCases = sentences.groupBy { recallSentence -> createRecallCourtCaseDetailsForGrouping(recallSentence) }
        .map { (group, groupedSentences) ->
          group.copy(
            sentences = groupedSentences.map {
              RecalledSentence(
                sentenceUuid = it.sentence.sentenceUuid,
                offenceCode = it.sentence.charge.offenceCode,
                offenceStartDate = it.sentence.charge.offenceStartDate,
                offenceEndDate = it.sentence.charge.offenceEndDate,
                sentenceDate = group.sentencingAppearanceDate,
                countNumber = it.sentence.countNumber,
                lineNumber = it.sentence.legacyData?.nomisLineReference,
                periodLengths = it.sentence.periodLengths.map { periodLength ->
                  PeriodLength(
                    years = periodLength.years,
                    months = periodLength.months,
                    weeks = periodLength.weeks,
                    days = periodLength.days,
                    periodOrder = periodLength.periodOrder,
                    periodLengthType = periodLength.periodLengthType,
                    legacyData = periodLength.legacyData,
                    periodLengthUuid = periodLength.periodLengthUuid,
                  )
                },
                sentenceServeType = it.sentence.sentenceServeType,
                sentenceTypeDescription = it.sentence.sentenceType?.description,
              )
            }.sortedBy { it.offenceStartDate },
          )
        },
      ual = ualAdjustment?.let { RecallUALAdjustment(it.id!!, it.days!!) },
      calculationRequestId = recall.calculationRequestId,
    )

    private fun createRecallCourtCaseDetailsForGrouping(recallSentence: RecallSentenceEntity): RecallCourtCaseDetails {
      val firstSentencingAppearance = recallSentence.sentence.charge.appearanceCharges
        .map { it.appearance!! }
        .filter { it.statusId != CourtAppearanceEntityStatus.DELETED && it.warrantType == "SENTENCING" }
        .minByOrNull { it.appearanceDate }
      return RecallCourtCaseDetails(
        firstSentencingAppearance?.courtCaseReference,
        firstSentencingAppearance?.courtCode,
        firstSentencingAppearance?.appearanceDate,
      )
    }
  }
}
