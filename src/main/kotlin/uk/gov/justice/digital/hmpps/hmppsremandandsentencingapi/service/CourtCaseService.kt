package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CourtCaseCountNumbers
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CourtCases
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.PeriodLength
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.paged.PagedCourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.recall.RecallableCourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.recall.RecallableCourtCaseSentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.recall.RecallableCourtCasesResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.RecordResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.util.EventMetadataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.error.ImmutableCourtCaseException
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtCaseEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.PagedCourtCaseOrderBy
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtCaseRepository
import java.time.LocalDate
import java.util.UUID

@Service
class CourtCaseService(private val courtCaseRepository: CourtCaseRepository, private val courtAppearanceService: CourtAppearanceService, private val serviceUserService: ServiceUserService, private val fixManyChargesToSentenceService: FixManyChargesToSentenceService) {

  @Transactional
  fun putCourtCase(createCourtCase: CreateCourtCase, caseUniqueIdentifier: String): RecordResponse<CourtCaseEntity> {
    var eventType = EventType.COURT_CASE_UPDATED
    val courtCase = courtCaseRepository.findByCaseUniqueIdentifier(caseUniqueIdentifier) ?: courtCaseRepository.save(CourtCaseEntity.from(createCourtCase, serviceUserService.getUsername(), caseUniqueIdentifier)).also { eventType = EventType.COURT_CASE_INSERTED }

    if (createCourtCase.prisonerId != courtCase.prisonerId) {
      throw ImmutableCourtCaseException("Cannot change prisoner id in a court case")
    }
    val (savedCourtCase, eventsToEmit) = saveCourtCaseAppearances(courtCase, createCourtCase)
    eventsToEmit.add(
      EventMetadataCreator.courtCaseEventMetadata(savedCourtCase.prisonerId, savedCourtCase.caseUniqueIdentifier, eventType),
    )
    return RecordResponse(
      savedCourtCase,
      eventsToEmit,
    )
  }

  @Transactional
  fun createCourtCase(createCourtCase: CreateCourtCase): RecordResponse<CourtCaseEntity> {
    val courtCase = courtCaseRepository.save(CourtCaseEntity.from(createCourtCase, serviceUserService.getUsername()))
    val (savedCourtCase, eventsToEmit) = saveCourtCaseAppearances(courtCase, createCourtCase)
    eventsToEmit.add(
      EventMetadataCreator.courtCaseEventMetadata(savedCourtCase.prisonerId, savedCourtCase.caseUniqueIdentifier, EventType.COURT_CASE_INSERTED),
    )
    return RecordResponse(
      savedCourtCase,
      eventsToEmit,
    )
  }

  private fun saveCourtCaseAppearances(courtCase: CourtCaseEntity, createCourtCase: CreateCourtCase): RecordResponse<CourtCaseEntity> {
    val toDeleteAppearances = courtCase.appearances.filter { existingCourtAppearance -> createCourtCase.appearances.none { it.appearanceUuid == existingCourtAppearance.appearanceUuid } }
    val eventsToEmit = toDeleteAppearances.flatMap { courtAppearanceService.deleteCourtAppearance(it).eventsToEmit }.toMutableSet()
    val appearanceRecords = createCourtCase.appearances.map { courtAppearanceService.createCourtAppearance(it, courtCase) }
    val appearances = appearanceRecords.map { it.record }.toSet()
    eventsToEmit.addAll(appearanceRecords.flatMap { it.eventsToEmit })
    courtCase.latestCourtAppearance = CourtAppearanceEntity.getLatestCourtAppearance(appearances)
    return RecordResponse(courtCaseRepository.save(courtCase), eventsToEmit)
  }

  @Transactional
  fun searchCourtCases(prisonerId: String, pageable: Pageable): RecordResponse<Page<CourtCase>> {
    val courtCasePage = courtCaseRepository.findByPrisonerIdAndLatestCourtAppearanceIsNotNullAndStatusIdNot(prisonerId, pageable = pageable)
    val eventsToEmit = fixManyChargesToSentenceService.fixCourtCaseSentences(courtCasePage.content)
    return RecordResponse(
      courtCasePage.map {
        CourtCase.from(it)
      },
      eventsToEmit,
    )
  }

  @Transactional
  fun pagedSearchCourtCases(prisonerId: String, pageable: Pageable, pagedCourtCaseOrderBy: PagedCourtCaseOrderBy): RecordResponse<Page<PagedCourtCase>> {
    val courtCaseRows = courtCaseRepository.searchCourtCases(
      prisonerId,
      pageable.pageSize,
      pageable.offset,
      pagedCourtCaseOrderBy,
      EntityStatus.ACTIVE,
      EntityStatus.DELETED,
    )
    val manyChargesToSentenceCourtCaseIds = courtCaseRows.filter { it.sentenceStatus == EntityStatus.MANY_CHARGES_DATA_FIX }.map { it.courtCaseId }.toSet()
    val eventsToEmit = fixManyChargesToSentenceService.fixCourtCasesById(manyChargesToSentenceCourtCaseIds)
    val toReturnCourtCases = if (eventsToEmit.isEmpty()) {
      courtCaseRows
    } else {
      courtCaseRepository.searchCourtCases(
        prisonerId,
        pageable.pageSize,
        pageable.offset,
        pagedCourtCaseOrderBy,
        EntityStatus.ACTIVE,
        EntityStatus.DELETED,
      )
    }
    val count = courtCaseRepository.countCourtCases(prisonerId)

    val courtCaseMap = toReturnCourtCases.groupBy { it.courtCaseId }
    val appearanceDateCompareTo = when (pagedCourtCaseOrderBy) {
      PagedCourtCaseOrderBy.STATUS_APPEARANCE_DATE_DESC -> compareBy<PagedCourtCase> { it.courtCaseStatus }.thenComparing(compareByDescending { it.latestCourtAppearance.warrantDate })
      PagedCourtCaseOrderBy.APPEARANCE_DATE_ASC -> compareBy<PagedCourtCase> { it.latestCourtAppearance.warrantDate }
      PagedCourtCaseOrderBy.APPEARANCE_DATE_DESC -> compareByDescending { it.latestCourtAppearance.warrantDate }
    }
    val pagedCourtCases = courtCaseMap.values.map { PagedCourtCase.from(it) }
      .sortedWith(appearanceDateCompareTo)
    return RecordResponse(PageImpl(pagedCourtCases, pageable, count), eventsToEmit)
  }

  @Transactional
  fun getCourtCaseByUuid(courtCaseUUID: String): RecordResponse<CourtCase>? = courtCaseRepository.findByCaseUniqueIdentifier(courtCaseUUID)?.let {
    val eventsToEmit = fixManyChargesToSentenceService.fixCourtCaseSentences(listOf(it))
    RecordResponse(CourtCase.from(it), eventsToEmit)
  }

  @Transactional
  fun getLatestAppearanceByCourtCaseUuid(courtCaseUUID: String): RecordResponse<CourtAppearance>? = courtCaseRepository.findByCaseUniqueIdentifier(courtCaseUUID)?.let {
    val eventsToEmit = fixManyChargesToSentenceService.fixCourtCaseSentences(listOf(it))
    RecordResponse(CourtAppearance.from(it.latestCourtAppearance!!), eventsToEmit)
  }

  @Transactional
  fun getSentencedCourtCases(prisonerId: String): RecordResponse<CourtCases> = courtCaseRepository.findSentencedCourtCasesByPrisonerId(prisonerId).let {
    val eventsToEmit = fixManyChargesToSentenceService.fixCourtCaseSentences(it)
    RecordResponse(CourtCases.from(it), eventsToEmit)
  }

  @Transactional
  fun getRecallableCourtCases(
    prisonerId: String,
    sortBy: String = "date",
    sortOrder: String = "desc",
  ): RecordResponse<RecallableCourtCasesResponse> {
    val courtCases = courtCaseRepository.findSentencedCourtCasesByPrisonerId(
      prisonerId,
      sentenceStatuses = listOf(
        EntityStatus.ACTIVE,
        EntityStatus.INACTIVE,
      ),
    )
      .filter { courtCase ->
        courtCase.statusId == EntityStatus.ACTIVE &&
          courtCase.appearances.any { appearance ->
            appearance.statusId == EntityStatus.ACTIVE &&
              appearance.appearanceCharges.any { appearanceCharge ->
                appearanceCharge.charge?.statusId == EntityStatus.ACTIVE &&
                  appearanceCharge.charge?.sentences?.isNotEmpty() == true
              }
          }
      }

    val eventsToEmit = fixManyChargesToSentenceService.fixCourtCaseSentences(courtCases)

    val recallableCourtCases = courtCases
      .filter { it.latestCourtAppearance != null }
      .map { courtCase ->
        val latestAppearance = courtCase.latestCourtAppearance!!
        val firstDayInCustody = courtCase.appearances
          .filter { it.statusId == EntityStatus.ACTIVE }
          .minOfOrNull { it.appearanceDate }

        RecallableCourtCase(
          courtCaseUuid = courtCase.caseUniqueIdentifier,
          reference = latestAppearance.courtCaseReference ?: "",
          courtCode = latestAppearance.courtCode,
          status = courtCase.statusId,
          isSentenced = courtCase.appearances.any { appearance ->
            appearance.appearanceCharges.any { it.charge?.sentences?.isNotEmpty() == true }
          },
          sentences = courtCase.appearances
            .filter { it.statusId == EntityStatus.ACTIVE }
            .flatMap { appearance ->
              appearance.appearanceCharges
                .filter { it.charge?.statusId == EntityStatus.ACTIVE }
                .flatMap { it.charge?.sentences ?: emptyList() }
            }
            .map { sentence ->
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
                periodLengths = sentence.periodLengths.map { periodLength ->
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
              )
            },
          date = latestAppearance.appearanceDate,
          firstDayInCustody = firstDayInCustody,
        )
      }

    val sortedCases = when (sortBy.lowercase()) {
      "reference" -> when (sortOrder.lowercase()) {
        "asc" -> recallableCourtCases.sortedBy { it.reference }
        else -> recallableCourtCases.sortedByDescending { it.reference }
      }

      "court" -> when (sortOrder.lowercase()) {
        "asc" -> recallableCourtCases.sortedBy { it.courtCode }
        else -> recallableCourtCases.sortedByDescending { it.courtCode }
      }

      else -> when (sortOrder.lowercase()) {
        "asc" -> recallableCourtCases.sortedBy { it.date }
        else -> recallableCourtCases.sortedByDescending { it.date }
      }
    }

    return RecordResponse(
      RecallableCourtCasesResponse(
        cases = sortedCases,
      ),
      eventsToEmit,
    )
  }

  @Transactional(readOnly = true)
  fun getAllCountNumbers(courtCaseUuid: String): CourtCaseCountNumbers = CourtCaseCountNumbers.from(courtCaseRepository.findSentenceCountNumbers(courtCaseUuid))

  @Transactional(readOnly = true)
  fun getLatestOffenceDateForCourtCase(
    courtCaseUuid: String,
    appearanceUuidToExclude: String?,
  ): LocalDate? = if (appearanceUuidToExclude == null) {
    courtCaseRepository.findLatestOffenceDate(courtCaseUuid)
  } else {
    courtCaseRepository.findLatestOffenceDateExcludingAppearance(courtCaseUuid, UUID.fromString(appearanceUuidToExclude))
  }
}
