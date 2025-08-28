package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtCaseEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.SentenceHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtCaseRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.SentenceHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.merge.DeactivatedCourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.merge.DeactivatedSentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.merge.MergePerson
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.ServiceUserService
import java.util.UUID

@Service
class LegacyPrisonerMergeService(private val courtCaseRepository: CourtCaseRepository, private val serviceUserService: ServiceUserService, private val sentenceHistoryRepository: SentenceHistoryRepository) {

  @Transactional
  fun process(mergePerson: MergePerson, retainedPrisonerNumber: String) {
    val courtCases = courtCaseRepository.findAllByPrisonerId(mergePerson.removedPrisonerNumber)
    val deactivatedCourtCasesMap = mergePerson.casesDeactivated.associateBy { it.dpsCourtCaseUuid }
    val deactivatedSentencesMap = mergePerson.sentencesDeactivated.associateBy { it.dpsSentenceUuid }
    val trackingData = PrisonerMergeDataTracking(retainedPrisonerNumber, serviceUserService.getUsername())
    processExistingCourtCases(courtCases, deactivatedCourtCasesMap, trackingData)
    processSentences(courtCases, deactivatedSentencesMap, trackingData)
    auditRecords(trackingData)
  }

  fun processExistingCourtCases(courtCases: List<CourtCaseEntity>, deactivatedCourtCasesMap: Map<String, DeactivatedCourtCase>, trackingData: PrisonerMergeDataTracking) {
    courtCases.forEach { courtCase ->
      courtCase.prisonerId = trackingData.retainedPrisonerNumber
      deactivatedCourtCasesMap[courtCase.caseUniqueIdentifier]?.also {
        val newStatus = if (it.active) EntityStatus.ACTIVE else EntityStatus.INACTIVE
        courtCase.statusId = newStatus
      }
      trackingData.editedCourtCases.add(courtCase)
    }
  }

  fun processSentences(courtCases: List<CourtCaseEntity>, deactivatedSentencesMap: Map<UUID, DeactivatedSentence>, trackingData: PrisonerMergeDataTracking) {
    courtCases.flatMap { courtCase -> courtCase.appearances.flatMap { appearance -> appearance.appearanceCharges.filter { it.charge?.getActiveOrInactiveSentence() != null }.map { it.charge!!.getActiveOrInactiveSentence()!! } } }
      .filter { deactivatedSentencesMap.containsKey(it.sentenceUuid) }
      .forEach { sentenceEntity ->
        val active = deactivatedSentencesMap[sentenceEntity.sentenceUuid]!!.active
        var hasChanged = false
        if (sentenceEntity.statusId != EntityStatus.MANY_CHARGES_DATA_FIX) {
          val newStatus = if (active) EntityStatus.ACTIVE else EntityStatus.INACTIVE
          hasChanged = newStatus != sentenceEntity.statusId
          sentenceEntity.statusId = newStatus
        }
        hasChanged = hasChanged || active != sentenceEntity.legacyData?.active
        sentenceEntity.legacyData?.active = active
        if (hasChanged) {
          trackingData.editedSentences.add(sentenceEntity)
        }
      }
  }

  fun auditRecords(trackingData: PrisonerMergeDataTracking) {
    trackingData.editedSentences.forEach { sentenceEntity ->
      sentenceHistoryRepository.save(SentenceHistoryEntity.from(sentenceEntity))
    }
  }

  data class PrisonerMergeDataTracking(
    val retainedPrisonerNumber: String,
    val username: String,
    val editedCourtCases: MutableList<CourtCaseEntity> = mutableListOf(),
    val editedSentences: MutableList<SentenceEntity> = mutableListOf(),
  )
}
