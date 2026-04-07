package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import jakarta.persistence.EntityNotFoundException
import org.jetbrains.annotations.VisibleForTesting
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.AdjustmentsApiClient
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.AdjustmentDto
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.UnlawfullyAtLargeDto
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateRecall
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.DeleteRecallResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.IsRecallPossible
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.IsRecallPossibleRequest
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.IsRecallPossibleResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.PeriodLength
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.SaveRecallResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.recall.CourtCaseMergedGroups
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.recall.DuplicateSentenceKey
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.recall.DuplicateSentencePeriodLength
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.recall.Recall
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.recall.RecallableCourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.recall.RecallableCourtCaseSentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.recall.RecallableCourtCasesResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.recall.SentenceWithCaseUuid
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventMetadata
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.RecordResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.util.EventMetadataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.RecallEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.RecallSentenceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.RecallHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.RecallSentenceHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.SentenceHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ChangeSource
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ChargeEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.CourtAppearanceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.PeriodLengthEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.RecallEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.RecallType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceTypeClassification
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtCaseRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.RecallRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.RecallSentenceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.RecallTypeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.SentenceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.RecallHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.RecallSentenceHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.SentenceHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.service.LegacyRecallService
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.service.LegacySentenceService
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.UUID

@Service
class RecallService(
  private val recallRepository: RecallRepository,
  private val recallSentenceRepository: RecallSentenceRepository,
  private val recallTypeRepository: RecallTypeRepository,
  private val sentenceRepository: SentenceRepository,
  private val sentenceService: SentenceService,
  private val recallHistoryRepository: RecallHistoryRepository,
  private val recallSentenceHistoryRepository: RecallSentenceHistoryRepository,
  private val adjustmentsApiClient: AdjustmentsApiClient,
  private val sentenceHistoryRepository: SentenceHistoryRepository,
  private val serviceUserService: ServiceUserService,
  private val courtCaseRepository: CourtCaseRepository,
  private val fixManyChargesToSentenceService: FixManyChargesToSentenceService,
) {
  @Transactional
  fun createRecall(createRecall: CreateRecall, recallUuid: UUID? = null): RecordResponse<SaveRecallResponse> {
    val recallType = recallTypeRepository.findOneByCode(createRecall.recallTypeCode)
    val recall = recallRepository.save(RecallEntity.fromDps(createRecall, recallType!!, recallUuid))
    val recallHistory =
      recallHistoryRepository.save(RecallHistoryEntity.from(recall, ChangeSource.DPS))
    createRecall.sentenceIds?.let { sentenceIds ->
      sentenceRepository.findBySentenceUuidIn(sentenceIds)
        .forEach {
          val isPossibleForSentence = isRecallPossibleForSentence(it, createRecall.recallTypeCode)
          if (isPossibleForSentence != IsRecallPossible.YES) {
            throw IllegalStateException("Tried to create a recall for sentence (${it.sentenceUuid}) but not possible due to $isPossibleForSentence")
          }
          val recallSentence = recallSentenceRepository.save(RecallSentenceEntity.placeholderEntity(recall, it))
          it.statusId = SentenceEntityStatus.ACTIVE
          it.updatedAt = ZonedDateTime.now()
          it.updatedBy = createRecall.createdByUsername
          it.updatedPrison = createRecall.createdByPrison
          it.legacyData = it.legacyData?.copy(active = null)
          sentenceHistoryRepository.save(
            SentenceHistoryEntity.from(
              it,
              ChangeSource.DPS,
            ),
          )
          recallSentenceHistoryRepository.save(
            RecallSentenceHistoryEntity.from(
              recallHistory,
              recallSentence,
              ChangeSource.DPS,
            ),
          )
        }
    }
    if (doesRecallRequireUAL(createRecall.revocationDate, createRecall.returnToCustodyDate)) {
      adjustmentsApiClient.createAdjustments(
        listOf(
          createUalDtoForRecall(
            recall.prisonerId,
            recall.revocationDate!!,
            recall.returnToCustodyDate!!,
            recall.recallUuid,
          ),
        ),
      )
    }
    return RecordResponse(
      SaveRecallResponse.from(recall),
      mutableSetOf(
        EventMetadataCreator.recallEventMetadata(
          recall.prisonerId,
          recall.recallUuid.toString(),
          (createRecall.sentenceIds ?: listOf()).map { it.toString() },
          emptyList(),
          null,
          EventType.RECALL_INSERTED,
        ),
      ),
    )
  }

  @Transactional
  fun updateRecall(recallUuid: UUID, recall: CreateRecall): RecordResponse<SaveRecallResponse> {
    val recallToUpdate = recallRepository.findOneByRecallUuid(recallUuid)

    return if (recallToUpdate == null) {
      createRecall(recall, recallUuid)
    } else {
      val originalRevocationDate =
        requireNotNull(recallToUpdate.revocationDate) { "Can only update DPS recall which requires revocation date" }
      val originalRTCDate = recallToUpdate.returnToCustodyDate

      val recallTypeEntity = recallTypeRepository.findOneByCode(recall.recallTypeCode)!!

      val previousSentenceIds = recallToUpdate.recallSentences.map { it.sentence.sentenceUuid }

      val sentencesToDelete = previousSentenceIds.filterNot { (recall.sentenceIds ?: emptyList()).contains(it) }
      val sentencesToCreate = recall.sentenceIds?.filterNot { previousSentenceIds.contains(it) } ?: listOf()

      val recallSentencesToDelete = recallToUpdate.recallSentences
        .filter { sentencesToDelete.contains(it.sentence.sentenceUuid) }

      recallToUpdate.recallSentences.removeAll(recallSentencesToDelete)

      recallSentencesToDelete.forEach {
        deleteDpsRecallSentence(recallSentence = it, updatedPrison = recall.createdByPrison)
      }
      sentenceRepository.findBySentenceUuidIn(sentencesToCreate)
        .forEach {
          val isPossibleForSentence = isRecallPossibleForSentence(it, recall.recallTypeCode)
          if (isPossibleForSentence != IsRecallPossible.YES) {
            throw IllegalStateException("Tried to create a recall for sentence (${it.sentenceUuid}) but not possible due to $isPossibleForSentence")
          }
          recallSentenceRepository.save(RecallSentenceEntity.placeholderEntity(recallToUpdate, it))
          it.statusId = SentenceEntityStatus.ACTIVE
          it.updatedAt = ZonedDateTime.now()
          it.updatedBy = recall.createdByUsername
          it.updatedPrison = recall.createdByPrison
          it.legacyData = it.legacyData?.copy(active = null)
          sentenceHistoryRepository.save(
            SentenceHistoryEntity.from(it, ChangeSource.DPS),
          )
        }

      recallToUpdate.apply {
        revocationDate = recall.revocationDate
        returnToCustodyDate = recall.returnToCustodyDate
        inPrisonOnRevocationDate = recall.inPrisonOnRevocationDate
        recallType = recallTypeEntity
        updatedAt = ZonedDateTime.now()
        updatedBy = recall.createdByUsername
        updatedPrison = recall.createdByPrison
        calculationRequestId = recall.calculationRequestId
      }
      val recallHistoryEntity =
        recallHistoryRepository.save(
          RecallHistoryEntity.from(
            recallToUpdate,
            ChangeSource.DPS,
          ),
        )
      recallToUpdate.recallSentences.forEach {
        recallSentenceHistoryRepository.save(
          RecallSentenceHistoryEntity.from(recallHistoryEntity, it, ChangeSource.DPS).apply {},
        )
      }
      val savedRecall = recallRepository.save(recallToUpdate)
      updateAdjustmentsIfRequired(
        recallToUpdate.recallUuid,
        recallToUpdate.prisonerId,
        originalRevocationDate,
        originalRTCDate,
        recall.revocationDate,
        recall.returnToCustodyDate,
      )

      RecordResponse(
        SaveRecallResponse.from(savedRecall),
        mutableSetOf(
          EventMetadataCreator.recallEventMetadata(
            savedRecall.prisonerId,
            savedRecall.recallUuid.toString(),
            sentenceIds = (recall.sentenceIds ?: listOf()).map { it.toString() },
            previousSentenceIds = previousSentenceIds.map { it.toString() },
            null,
            EventType.RECALL_UPDATED,
          ),
        ),
      )
    }
  }

  private fun updateAdjustmentsIfRequired(
    recallUuid: UUID,
    prisonerId: String,
    originalRevocationDate: LocalDate,
    originalRTCDate: LocalDate?,
    newRevocationDate: LocalDate,
    newRTCDate: LocalDate?,
  ) {
    val originalRequiresAdjustment = doesRecallRequireUAL(originalRevocationDate, originalRTCDate)
    val newRequiresAdjustment = doesRecallRequireUAL(newRevocationDate, newRTCDate)
    if (originalRequiresAdjustment && !newRequiresAdjustment) {
      adjustmentsApiClient.getRecallAdjustment(prisonerId, recallUuid)?.id?.let {
        adjustmentsApiClient.deleteAdjustment(it)
      }
    } else if (!originalRequiresAdjustment && newRequiresAdjustment) {
      adjustmentsApiClient.createAdjustments(
        listOf(
          createUalDtoForRecall(
            prisonerId,
            newRevocationDate,
            newRTCDate!!,
            recallUuid,
          ),
        ),
      )
    } else if (originalRequiresAdjustment && (originalRevocationDate != newRevocationDate || originalRTCDate != newRTCDate)) {
      val originalAdjustment = requireNotNull(
        adjustmentsApiClient.getRecallAdjustment(
          prisonerId,
          recallUuid,
        ),
      ) { "Original adjustment is missing" }
      adjustmentsApiClient.updateAdjustment(
        createUalDtoForRecall(
          prisonerId,
          newRevocationDate,
          newRTCDate!!,
          recallUuid,
        ).copy(id = originalAdjustment.id),
      )
    }
  }

  @Transactional
  fun deleteRecall(recallUuid: UUID): RecordResponse<DeleteRecallResponse> {
    val recallToDelete = recallRepository.findOneByRecallUuid(recallUuid)
      ?: throw EntityNotFoundException("Recall not found $recallUuid")

    val isLegacyRecall =
      recallToDelete.recallSentences.all { it.sentence.sentenceType?.sentenceTypeUuid == LegacySentenceService.recallSentenceTypeBucketUuid }
    val isOnlyRecall = recallToDelete.recallSentences.all { it.sentence.recallSentences.size == 1 }

    val eventsToEmit = mutableListOf<EventMetadata>()
    var previousRecall: RecallEntity?
    val ualAdjustmentToDelete: AdjustmentDto? = if (!isLegacyRecall) {
      adjustmentsApiClient.getRecallAdjustment(recallToDelete.prisonerId, recallUuid)
    } else {
      null
    }
    if (isLegacyRecall && isOnlyRecall) {
      eventsToEmit.addAll(deleteLegacyRecallSentenceAndAssociatedRecall(recallToDelete))
    } else {
      previousRecall = recallToDelete.recallSentences.map {
        it.sentence
      }.flatMap { it.recallSentences }
        .map { it.recall }
        .filter { it.recallUuid != recallUuid }
        .maxByOrNull { it.createdAt }

      // deleting the sentence for the legacy recall will delete the recall so it's only required here for DPS
      recallToDelete.status = RecallEntityStatus.DELETED
      recallToDelete.updatedBy = serviceUserService.getUsername()
      recallToDelete.updatedPrison = null // unknown on delete
      recallToDelete.updatedAt = ZonedDateTime.now()
      recallToDelete.recallSentences.forEach { recallSentence ->
        deleteDpsRecallSentence(recallSentence)
      }
      recallHistoryRepository.save(
        RecallHistoryEntity.from(
          recallToDelete,
          ChangeSource.DPS,
        ),
      )
      recallRepository.save(recallToDelete)
      eventsToEmit.add(
        EventMetadataCreator.recallEventMetadata(
          recallToDelete.prisonerId,
          recallToDelete.recallUuid.toString(),
          recallToDelete.recallSentences.map { it.sentence.sentenceUuid.toString() }.distinct(),
          emptyList(),
          previousRecall?.recallUuid?.toString(),
          EventType.RECALL_DELETED,
        ),
      )
    }
    if (ualAdjustmentToDelete?.id != null) {
      adjustmentsApiClient.deleteAdjustment(ualAdjustmentToDelete.id)
    }
    return RecordResponse(
      DeleteRecallResponse.from(recallToDelete),
      (eventsToEmit).toMutableSet(),
    )
  }

  // This gets called on UPDATE and DELETE of a recall. The updatedPrison is unknown on DELETE
  private fun deleteDpsRecallSentence(recallSentence: RecallSentenceEntity, updatedPrison: String? = null) {
    recallSentence.preRecallSentenceStatus?.let { preRecallSentenceStatus ->
      recallSentence.sentence.statusId = preRecallSentenceStatus
      recallSentence.sentence.legacyData = recallSentence.sentence.legacyData?.copy(active = null)
      recallSentence.sentence.updatedAt = ZonedDateTime.now()
      recallSentence.sentence.updatedBy = serviceUserService.getUsername()
      recallSentence.sentence.updatedPrison = updatedPrison

      sentenceHistoryRepository.save(
        SentenceHistoryEntity.from(
          recallSentence.sentence,
          ChangeSource.DPS,
        ),
      )
    }

    recallSentenceRepository.delete(recallSentence)
  }

  private fun deleteLegacyRecallSentenceAndAssociatedRecall(recallToDelete: RecallEntity): List<EventMetadata> = recallToDelete.recallSentences.flatMap {
    val appearance = it.sentence.charge.appearanceCharges.first().appearance
    if (appearance != null) {
      sentenceService.deleteSentence(
        it.sentence,
        it.sentence.charge,
        recallToDelete.prisonerId,
        appearance.courtCase.caseUniqueIdentifier,
        appearance.appearanceUuid.toString(),
      ).eventsToEmit
    } else {
      emptyList()
    }
  }

  @Transactional(readOnly = true)
  fun findRecallByUuid(recallUuid: UUID): Recall {
    val recall = recallRepository.findOneByRecallUuid(recallUuid)
      ?: throw EntityNotFoundException("No recall exists for the passed in UUID")
    val recallSentences = recallSentenceRepository.findByRecallId(recall.id).orEmpty()
    val adjustment =
      if (recall.revocationDate != null && doesRecallRequireUAL(recall.revocationDate!!, recall.returnToCustodyDate)) {
        adjustmentsApiClient.getRecallAdjustment(recall.prisonerId, recall.recallUuid)
      } else {
        null
      }
    return Recall.from(recall, recallSentences, adjustment)
  }

  @Transactional(readOnly = true)
  fun findRecallsByPrisonerId(prisonerId: String): List<Recall> {
    val recallAdjustments = adjustmentsApiClient.getAdjustments(prisonerId).filter { it.recallId != null }
    return recallRepository.findByPrisonerIdAndStatus(prisonerId).map { recall ->
      val recallSentences = recallSentenceRepository.findByRecallId(recall.id).orEmpty()
      Recall.from(recall, recallSentences, recallAdjustments.find { it.recallId == recall.recallUuid.toString() })
    }
  }

  private fun createUalDtoForRecall(
    prisonerId: String,
    revocationDate: LocalDate,
    rtcDate: LocalDate,
    recallUuid: UUID,
  ): AdjustmentDto = AdjustmentDto(
    id = null,
    person = prisonerId,
    adjustmentType = "UNLAWFULLY_AT_LARGE",
    fromDate = revocationDate.plusDays(1),
    toDate = rtcDate.minusDays(1),
    days = null,
    recallId = recallUuid.toString(),
    unlawfullyAtLarge = UnlawfullyAtLargeDto(),
  )

  @Transactional(readOnly = true)
  fun isRecallPossible(request: IsRecallPossibleRequest): IsRecallPossibleResponse {
    val sentences = sentenceRepository.findBySentenceUuidIn(request.sentenceIds)
    val isPossibleForEachSentence = sentences.map { it to isRecallPossibleForSentence(it, request.recallType) }
    val isPossible = isPossibleForEachSentence.map { it.second }.minBy { it.priority }
    val notPossibleSentences =
      isPossibleForEachSentence.filter { it.second === isPossible }.map { it.first.sentenceUuid }

    return IsRecallPossibleResponse(
      isPossible,
      if (isPossible != IsRecallPossible.YES) notPossibleSentences else emptyList(),
    )
  }

  private fun isRecallPossibleForSentence(sentence: SentenceEntity, recallType: RecallType): IsRecallPossible {
    if (sentence.sentenceType == null) {
      return isRecallPossibleUsingLegacySentenceCalcType(sentence.legacyData?.sentenceCalcType, recallType)
    }
    if (sentence.sentenceType!!.sentenceTypeUuid == LegacySentenceService.recallSentenceTypeBucketUuid) {
      return isRecallPossibleForLegacyRecall(sentence, recallType)
    }
    return isRecallPossibleForClassification(sentence.sentenceType!!.classification, recallType)
  }

  private fun isRecallPossibleUsingLegacySentenceCalcType(
    sentenceCalcType: String?,
    recallType: RecallType,
  ): IsRecallPossible {
    if (sentenceCalcType == null) {
      return IsRecallPossible.RECALL_TYPE_AND_SENTENCE_MAPPING_NOT_POSSIBLE
    }

    val classification = nomisSentenceCalcTypeToClassification(sentenceCalcType)
    return if (classification == SentenceTypeClassification.UNKNOWN) {
      IsRecallPossible.RECALL_TYPE_AND_SENTENCE_MAPPING_NOT_POSSIBLE
    } else {
      isRecallPossibleForClassification(classification, recallType)
    }
  }

  private fun isRecallPossibleForClassification(
    classification: SentenceTypeClassification,
    recallType: RecallType,
  ): IsRecallPossible = when (classification) {
    SentenceTypeClassification.STANDARD -> IsRecallPossible.YES
    SentenceTypeClassification.EXTENDED, SentenceTypeClassification.INDETERMINATE -> if (recallType == RecallType.LR) IsRecallPossible.YES else IsRecallPossible.RECALL_TYPE_AND_SENTENCE_MAPPING_NOT_POSSIBLE
    SentenceTypeClassification.SOPC -> if (listOf(
        RecallType.LR,
        RecallType.FTR_28,
      ).contains(recallType)
    ) {
      IsRecallPossible.YES
    } else {
      IsRecallPossible.RECALL_TYPE_AND_SENTENCE_MAPPING_NOT_POSSIBLE
    }

    else -> IsRecallPossible.RECALL_TYPE_AND_SENTENCE_MAPPING_NOT_POSSIBLE
  }

  /* Is the new DPS recall possible for a sentence which has previously been recalled in NOMIS. */
  private fun isRecallPossibleForLegacyRecall(
    sentence: SentenceEntity,
    recallType: RecallType,
  ): IsRecallPossible {
    val latestRecall = sentence.earliestRecall()!!

    val recallLegacyData =
      latestRecall.let { recall -> sentence.recallSentences.find { it.recall.id == recall.id }?.legacyData }!!
    val sentenceCalcType = recallLegacyData.sentenceCalcType

    val classification =
      LegacyRecallService.classificationToLegacySentenceTypeMap.mapNotNull { (classification, types) ->
        if (types.contains(sentenceCalcType)) {
          classification
        } else {
          null
        }
      }.firstOrNull()

    if (classification != null) {
      return isRecallPossibleForClassification(classification, recallType)
    } else {
      // Legacy recall can match more than one sentence type, but will all be standard sentences.
      if (recallType == RecallType.LR && unknownPreRecallSentenceTypes.contains(sentenceCalcType)) {
        return IsRecallPossible.UNKNOWN_PRE_RECALL_MAPPING
      }
    }
    return IsRecallPossible.YES
  }

  @Transactional
  fun getRecallableCourtCases(
    prisonerId: String,
    mergeDuplicateCourtCases: Boolean = false,
  ): RecordResponse<RecallableCourtCasesResponse> {
    val courtCasesWithAnAppearance = courtCaseRepository.findSentencedCourtCasesByPrisonerId(
      prisonerId,
    ).filter { it.latestCourtAppearance != null }

    val eventsToEmit = fixManyChargesToSentenceService.fixCourtCaseSentences(courtCasesWithAnAppearance)

    val recallableCourtCases = courtCasesWithAnAppearance
      .map { courtCase ->
        val latestAppearance = courtCase.latestCourtAppearance!!

        val activeAppearances = courtCase.appearances
          .filter { it.statusId == CourtAppearanceEntityStatus.ACTIVE }

        val firstDayInCustody = activeAppearances
          .minOfOrNull { it.appearanceDate }

        val firstSentencingAppearance = activeAppearances
          .first { appearance -> appearance.warrantType == "SENTENCING" }

        val activeAndInactiveSentencesWithAppearances = activeAppearances.flatMap { appearance ->
          appearance.appearanceCharges
            .filter { it.charge?.statusId == ChargeEntityStatus.ACTIVE && it.charge?.getLiveSentence() != null }
            .map { it.charge!!.getLiveSentence()!! to appearance }
        }

        RecallableCourtCase(
          courtCaseUuid = courtCase.caseUniqueIdentifier,
          reference = latestAppearance.courtCaseReference ?: "",
          courtCode = latestAppearance.courtCode,
          status = courtCase.statusId,
          isSentenced = activeAndInactiveSentencesWithAppearances.isNotEmpty(),
          sentences = activeAndInactiveSentencesWithAppearances.map { (sentence, appearance) ->
            val sentenceAppearance = if (appearance.warrantType == "SENTENCING") {
              appearance
            } else {
              firstSentencingAppearance
            }
            RecallableCourtCaseSentence(
              sentenceUuid = sentence.sentenceUuid,
              offenceCode = sentence.charge.offenceCode,
              offenceStartDate = sentence.charge.offenceStartDate,
              offenceEndDate = sentence.charge.offenceEndDate,
              outcome = sentence.charge.chargeOutcome?.outcomeName ?: sentence.charge.legacyData?.outcomeDescription,
              sentenceType = sentence.sentenceType?.description,
              sentenceTypeUuid = sentence.sentenceType?.sentenceTypeUuid.toString(),
              classification = sentence.sentenceType?.classification,
              systemOfRecord = "RAS",
              fineAmount = sentence.fineAmount,
              periodLengths = sentence.periodLengths
                .filter { it.statusId != PeriodLengthEntityStatus.DELETED }
                .map { periodLength ->
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
              convictionDate = sentence.convictionDate,
              chargeLegacyData = sentence.charge.legacyData,
              countNumber = sentence.countNumber,
              lineNumber = sentence.legacyData?.nomisLineReference,
              sentenceServeType = sentence.sentenceServeType,
              sentenceLegacyData = sentence.legacyData,
              outcomeDescription = sentence.charge.chargeOutcome?.outcomeName,
              isRecallable = sentence.sentenceType?.isRecallable ?: true,
              sentenceDate = sentenceAppearance.appearanceDate,
              consecutiveToSentenceUuid = sentence.consecutiveTo?.sentenceUuid,
              createdAt = sentence.legacyData?.postedDate
                ?.let { minOf(LocalDateTime.parse(it), sentence.createdAt.toLocalDateTime()) }
                ?: sentence.createdAt.toLocalDateTime(),
            )
          },
          appearanceDate = firstSentencingAppearance.appearanceDate,
          firstDayInCustody = firstDayInCustody,
        )
      }
      .filter { it.sentences.any { s -> s.isRecallable } }

    return if (!mergeDuplicateCourtCases) {
      RecordResponse(
        record = RecallableCourtCasesResponse(cases = recallableCourtCases.sortedByDescending { it.appearanceDate }),
        eventsToEmit = eventsToEmit,
      )
    } else {
      RecordResponse(
        record = RecallableCourtCasesResponse(cases = mergeAndSortCourtCases(recallableCourtCases)),
        eventsToEmit = eventsToEmit,
      )
    }
  }

  @VisibleForTesting
  fun mergeAndSortCourtCases(cases: List<RecallableCourtCase>): List<RecallableCourtCase> {
    if (cases.isEmpty()) return emptyList()

    val caseByUuid = cases.associateBy { it.courtCaseUuid }
    val sentencesByDupKey = buildSentencesByDupKey(cases, caseByUuid)
    val duplicateKeys = findDuplicateKeys(sentencesByDupKey)
    if (duplicateKeys.isEmpty()) return cases.sortedByDescending { it.appearanceDate }

    val sentencesByKey = pickLatestSentencePerDuplicateKey(sentencesByDupKey)
    val sentencesPerCase = sentencesByCaseUuid(sentencesByKey)
    val mergeGroups = groupCourtCasesToMergeByDuplicateKeys(cases, sentencesByDupKey, duplicateKeys)

    val duplicateWinnerRefs = duplicateKeys.mapNotNull { sentencesByKey[it] }

    val mergedCases = mergeGroups.mapNotNull { memberUuids ->
      val repUuid = chooseRepresentative(memberUuids, duplicateWinnerRefs)
      val rep = caseByUuid[repUuid] ?: return@mapNotNull null

      val mergedSentences = memberUuids
        .flatMap { sentencesPerCase[it].orEmpty() }
        .distinctBy { it.sentenceUuid }
        .sortedWith(compareBy<RecallableCourtCaseSentence> { it.createdAt }.thenBy { it.sentenceUuid })

      if (mergedSentences.isEmpty()) return@mapNotNull null

      rep.copy(sentences = mergedSentences, isSentenced = true)
    }

    return mergedCases.sortedByDescending { it.appearanceDate }
  }

  private fun buildSentencesByDupKey(
    cases: List<RecallableCourtCase>,
    caseByUuid: Map<String, RecallableCourtCase>,
  ): Map<DuplicateSentenceKey, List<SentenceWithCaseUuid>> = cases
    .flatMap { cc -> cc.sentences.map { s -> SentenceWithCaseUuid(cc.courtCaseUuid, s) } }
    .groupBy { sentence ->
      val courtCode = caseByUuid.getValue(sentence.caseUuid).courtCode
      DuplicateSentenceKey(
        courtCode = courtCode,
        offenceCode = sentence.sentence.offenceCode,
        offenceStartDate = sentence.sentence.offenceStartDate,
        sentenceDate = sentence.sentence.sentenceDate,
        periodLengths = sentence.sentence.periodLengths
          .map {
            DuplicateSentencePeriodLength(
              periodLengthType = it.periodLengthType.name,
              years = it.years,
              months = it.months,
              weeks = it.weeks,
              days = it.days,
            )
          }
          .sortedWith(
            compareBy<DuplicateSentencePeriodLength> { it.periodLengthType }
              .thenBy { it.years }
              .thenBy { it.months }
              .thenBy { it.weeks }
              .thenBy { it.days },
          ),
      )
    }

  private fun findDuplicateKeys(
    sentencesByDuplicateSentenceKey: Map<DuplicateSentenceKey, List<SentenceWithCaseUuid>>,
  ): Set<DuplicateSentenceKey> = sentencesByDuplicateSentenceKey
    .filter { (_, refs) -> refs.map { it.caseUuid }.distinct().size > 1 }
    .keys

  private fun pickLatestSentencePerDuplicateKey(
    sentencesByDuplicateSentenceKey: Map<DuplicateSentenceKey, List<SentenceWithCaseUuid>>,
  ): Map<DuplicateSentenceKey, SentenceWithCaseUuid> = sentencesByDuplicateSentenceKey.mapValues { (_, refs) ->
    refs.maxWith(compareBy<SentenceWithCaseUuid> { it.sentence.createdAt }.thenBy { it.caseUuid })
  }

  private fun sentencesByCaseUuid(
    sentencesByKey: Map<DuplicateSentenceKey, SentenceWithCaseUuid>,
  ): Map<String, List<RecallableCourtCaseSentence>> = sentencesByKey.values
    .groupBy { it.caseUuid }
    .mapValues { (_, refs) -> refs.map { it.sentence } }

  private fun groupCourtCasesToMergeByDuplicateKeys(
    cases: List<RecallableCourtCase>,
    sentencesByDuplicateSentenceKey: Map<DuplicateSentenceKey, List<SentenceWithCaseUuid>>,
    duplicateKeys: Set<DuplicateSentenceKey>,
  ): List<List<String>> {
    val courtCaseMergedGroups = CourtCaseMergedGroups(cases.map { it.courtCaseUuid })

    sentencesByDuplicateSentenceKey
      .filterKeys { it in duplicateKeys }
      .values
      .forEach { refs ->
        val caseUuids = refs.map { it.caseUuid }.distinct()
        val first = caseUuids.first()
        caseUuids.drop(1).forEach { courtCaseMergedGroups.mergeCasesIntoGroup(first, it) }
      }

    return cases
      .map { it.courtCaseUuid }
      .groupBy { courtCaseMergedGroups.findRepresentativeCase(it) }
      .values
      .toList()
  }

  private fun chooseRepresentative(
    memberUuids: List<String>,
    duplicateWinnerRefs: List<SentenceWithCaseUuid>,
  ): String = duplicateWinnerRefs
    .asSequence()
    .filter { it.caseUuid in memberUuids }
    .maxWithOrNull(compareBy<SentenceWithCaseUuid> { it.sentence.createdAt }.thenBy { it.caseUuid })
    ?.caseUuid
    ?: memberUuids.first()

  companion object {
    val unknownPreRecallSentenceTypes = listOf(
      "CUR",
      "CUR_ORA",
      "FTR",
      "FTR_HDC",
      "FTR_HDC_ORA",
      "HDR",
      "FTR_56ORA",
    )

    private fun nomisSentenceCalcTypeToClassification(type: String): SentenceTypeClassification = when (type.trim()) {
      "AR", "CR", "YOI" -> SentenceTypeClassification.STANDARD
      "EXT", "PPEXT_SENT" -> SentenceTypeClassification.EXTENDED
      "ALP", "ALP_CODE21", "DFL", "DLP", "HMPL", "LIFE", "MLP", "SEC93", "SEC94" -> SentenceTypeClassification.INDETERMINATE
      "CIVIL", "CIVILLT", "DTO", "TISCS", "YRO", "IC", "NP", "ZMD", "DPP", "AGG-IND", "LIFE/IPP" -> SentenceTypeClassification.CIVIL
      else -> SentenceTypeClassification.UNKNOWN
    }
  }
}
