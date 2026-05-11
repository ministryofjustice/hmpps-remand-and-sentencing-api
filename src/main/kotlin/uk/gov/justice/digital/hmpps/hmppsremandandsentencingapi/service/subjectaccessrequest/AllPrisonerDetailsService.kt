package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.subjectaccessrequest

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.SarContent
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.alldata.Charge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.alldata.CourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.alldata.CourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.alldata.ImmigrationDetention
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.alldata.PeriodLength
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.alldata.Prisoner
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.alldata.Recall
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.alldata.Sentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.subjectaccessrequest.alldata.ChargeSarEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.subjectaccessrequest.alldata.CourtAppearanceSarEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.subjectaccessrequest.alldata.CourtCaseSarEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.subjectaccessrequest.alldata.ImmigrationDetentionSarEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.subjectaccessrequest.alldata.RecallSarEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.subjectaccessrequest.alldata.SentenceSarEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.subjectaccessrequest.alldata.CourtCaseSarRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.subjectaccessrequest.alldata.ImmigrationDetentionSarRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.subjectaccessrequest.alldata.RecallSarRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.CourtRegisterService
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.PersonService
import java.time.LocalDate

class AllPrisonerDetailsService(
  private val courtCaseSarRepository: CourtCaseSarRepository,
  private val recallSarRepository: RecallSarRepository,
  private val immigrationDetentionSarRepository: ImmigrationDetentionSarRepository,
  private val personService: PersonService,
  private val courtRegisterService: CourtRegisterService,
) : PrisonerDetailsService {
  override fun getPrisonerDetails(
    prisonerNumber: String,
    from: LocalDate?,
    to: LocalDate?,
  ): SarContent? = courtCaseSarRepository.existsByPrisonerId(prisonerNumber).takeIf { it }?.let {
    val courtCases = mapCourtCases(courtCaseSarRepository.findByPrisonerId(prisonerNumber), from, to)
    val recalls = mapRecalls(recallSarRepository.findByPrisonerId(prisonerNumber), from, to)
    val immigrationDetentions = mapImmigrationDetentions(immigrationDetentionSarRepository.findByPrisonerId(prisonerNumber))
    val personDetails = personService.getPersonDetailsByPrisonerIdCached(prisonerNumber)
    val prisonerName = personDetails?.let { "${personDetails.firstName} ${personDetails.lastName}" }

    return Prisoner(prisonerNumber, prisonerName, courtCases, recalls, immigrationDetentions)
  }

  private fun mapImmigrationDetentions(immigrationDetentionSarEntities: List<ImmigrationDetentionSarEntity>): List<ImmigrationDetention> {
    val immigrationDetentions = mutableListOf<ImmigrationDetention>()

    immigrationDetentionSarEntities.forEach { immigrationDetentionSarEntity ->
      immigrationDetentions.add(
        ImmigrationDetention(
          immigrationDetentionSarEntity.immigrationDetentionRecordType,
          immigrationDetentionSarEntity.homeOfficeReferenceNumber,
          immigrationDetentionSarEntity.recordDate,
          immigrationDetentionSarEntity.noLongerOfInterestReason,
          immigrationDetentionSarEntity.noLongerOfInterestComment,
        ),
      )
    }
    return immigrationDetentions
  }

  private fun mapRecalls(recallSarEntities: List<RecallSarEntity>, from: LocalDate?, to: LocalDate?): List<Recall> {
    val recalls = mutableListOf<Recall>()

    recallSarEntities.filter { p -> filterByDate(from, to, p.revocationDate) }.forEach { recallSarEntity ->
      recalls.add(
        Recall(
          recallSarEntity.recallType.code,
          recallSarEntity.revocationDate,
          recallSarEntity.returnToCustodyDate,
          recallSarEntity.inPrisonOnRevocationDate,
          recallSarEntity.status,
        ),
      )
    }

    return recalls
  }

  private fun mapCourtCases(courtCaseEntities: List<CourtCaseSarEntity>, from: LocalDate?, to: LocalDate?): List<CourtCase> {
    val courtCases = mutableListOf<CourtCase>()
    courtCaseEntities.forEach { courtCaseEntity ->

      val chargeSarEntities = courtCaseEntity.latestCourtAppearance?.appearanceCharges
        ?.map { appearanceCharge -> appearanceCharge.charge }?.toList()

      val charges: List<Charge> = mapCharges(chargeSarEntities)
      val courtAppearance: CourtAppearance? = mapCourtAppearance(charges, courtCaseEntity.latestCourtAppearance, from, to)
      val courtAppearances: List<CourtAppearance> = courtCaseEntity.appearances.mapNotNull { appearanceEntity -> mapCourtAppearance(charges, appearanceEntity, from, to) }
      val courtCase: CourtCase = mapCourtCase(courtCaseEntity, courtAppearance, courtAppearances)

      courtCases.add(courtCase)
    }

    return courtCases
  }

  private fun mapCourtCase(
    courtCaseEntity: CourtCaseSarEntity,
    latestCourtAppearance: CourtAppearance?,
    courtAppearances: List<CourtAppearance>,
  ): CourtCase {
    val courtRegister = courtCaseEntity.latestCourtAppearance?.courtCode?.let {
      courtRegisterService.getCourtRegisterByCourtCodeCached(it)
    }

    return CourtCase(
      courtRegister?.courtName,
      courtCaseEntity.statusId,
      courtCaseEntity.createdAt,
      courtCaseEntity.updatedAt,
      latestCourtAppearance,
      courtAppearances,
    )
  }

  private fun mapCourtAppearance(
    charges: List<Charge>,
    latestCourtAppearance: CourtAppearanceSarEntity?,
    from: LocalDate?,
    to: LocalDate?,
  ): CourtAppearance? = filterByDate(from, to, latestCourtAppearance?.appearanceDate).takeIf { it }?.let {
    val appearanceDate = latestCourtAppearance?.appearanceDate
    val appearanceOutcomeName = latestCourtAppearance?.appearanceOutcome?.outcomeName
    val warrantyType = latestCourtAppearance?.warrantType
    val convictionDate = latestCourtAppearance?.overallConvictionDate
    val nextCourtAppearanceDate = latestCourtAppearance?.nextCourtAppearance?.appearanceDate

    return CourtAppearance(
      appearanceDate,
      appearanceOutcomeName,
      warrantyType,
      convictionDate,
      nextCourtAppearanceDate,
      charges,
    )
  }

  private fun mapCharges(chargeSarEntities: List<ChargeSarEntity?>?): List<Charge> {
    val charges = mutableListOf<Charge>()
    chargeSarEntities?.forEach { chargeSarEntity ->
      val offenceDesc = chargeSarEntity?.legacyData?.offenceDescription
      charges.add(
        Charge(
          chargeSarEntity?.offenceCode,
          offenceDesc,
          chargeSarEntity?.terrorRelated,
          chargeSarEntity?.foreignPowerRelated,
          chargeSarEntity?.domesticViolenceRelated,
          chargeSarEntity?.offenceStartDate,
          chargeSarEntity?.offenceEndDate,
          chargeSarEntity?.chargeOutcome?.outcomeName,
          mapSentence(chargeSarEntity?.getLiveSentence()),
        ),
      )
    }
    return charges
  }

  private fun mapSentence(sentenceSar: SentenceSarEntity?): Sentence {
    if (sentenceSar == null) {
      return Sentence()
    }

    val periodLengths = mutableListOf<PeriodLength>()
    val sentenceTypeDescription = sentenceSar.sentenceType?.description
    val sentenceTypeClassification = sentenceSar.sentenceType?.classification
    sentenceSar.periodLengths.forEach { periodLength ->
      periodLengths.add(
        PeriodLength(
          periodLength.years ?: 0,
          periodLength.months ?: 0,
          periodLength.weeks ?: 0,
          periodLength.days ?: 0,
          periodLength.periodOrder,
        ),
      )
    }
    val sentenceServeType = sentenceSar.sentenceServeType

    return Sentence(sentenceTypeDescription, sentenceTypeClassification, periodLengths, sentenceServeType)
  }
}
