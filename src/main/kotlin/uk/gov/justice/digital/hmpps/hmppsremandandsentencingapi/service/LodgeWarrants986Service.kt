package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.AgencyDetails
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.documents.CreateLodgeWarrants986
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.SentenceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document.DocumentGeneratorService
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document.LodgeWarrants986
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document.dto.PeriodLength
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.externalapi.CourtRegisterService
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.externalapi.ManageOffencesService
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.externalapi.PersonRecordService
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.externalapi.PrisonRegisterService
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.externalapi.PrisonSearchService
import java.io.InputStream
import java.time.LocalDate

@Service
class LodgeWarrants986Service(
  private val sentenceRepository: SentenceRepository,
  private val documentGeneratorService: DocumentGeneratorService,
  private val courtRegisterService: CourtRegisterService,
  private val prisonRegisterService: PrisonRegisterService,
  private val manageOffencesService: ManageOffencesService,
  private val personRecordService: PersonRecordService,
  private val prisonSearchService: PrisonSearchService,
) {

  fun renderDocument(createLodgeWarrants986: CreateLodgeWarrants986): InputStream {
    val imprisonmentInDefaultOfFineList = sentenceRepository.findCourtCodesForImprisonmentInDefaultOfFine(createLodgeWarrants986.prisonerId)
    require(imprisonmentInDefaultOfFineList.isNotEmpty()) {
      "No imprisonment in default of fine sentences found for prisoner ${createLodgeWarrants986.prisonerId}"
    }

    val imprisonmentInDefaultOfFine = imprisonmentInDefaultOfFineList.first()
    val courtRegister = courtRegisterService.getCourtRegisterByCourtCodeCached(imprisonmentInDefaultOfFine.courtCode)
    val personRecord = personRecordService.getPersonPrison(createLodgeWarrants986.prisonerId)
    val courtName = courtRegister?.courtName
    val name = "${personRecord?.firstName} ${personRecord?.lastName}".trim()
    val prison = prisonSearchService.getPrisoner(createLodgeWarrants986.prisonerId)
    val prisonName = prison?.prisonName
    val courtAddress: AgencyDetails.Address? = courtRegister?.let {
      prisonRegisterService.getAgencyDetails(it.courtId)?.addresses?.first()
    }
    val prisonTelephone: AgencyDetails.Telephone? = prison?.let {
      prisonRegisterService.getAgencyDetails(prison.prisonId)?.phones?.first()
    }

    val sentenceList = imprisonmentInDefaultOfFineList.groupBy { it.sentenceId }.map { (_, sentenceList) ->
      val periodLengths = sentenceList.map {
        PeriodLength.fromNullable(it.years, it.months, it.weeks, it.days, it.periodOrder)
      }
      val sentence = sentenceList.first()
      LodgeWarrants986.Sentence(
        periodLengths,
        manageOffencesService.getOffenceDetails(sentence.offenceCode)?.description ?: "Offence Code (${sentence.offenceCode})",
        sentence.fineAmount?.toDouble() ?: 0.0,
      )
    }

    val lodgeWarrants986 = LodgeWarrants986(
      LodgeWarrants986.Data(
        name,
        createLodgeWarrants986.prisonerId,
        LodgeWarrants986.Court(
          courtName,
          courtAddress?.addressLine1 ?: createLodgeWarrants986.courtPremise ?: "",
          courtAddress?.addressLine2 ?: createLodgeWarrants986.courtStreet ?: "",
          courtAddress?.town ?: createLodgeWarrants986.courtTown ?: "",
          courtAddress?.county ?: createLodgeWarrants986.courtCounty ?: "",
          courtAddress?.postcode ?: createLodgeWarrants986.courtPostalCode ?: "",
        ),
        LocalDate.now(),
        sentenceList,
        prisonTelephone?.number ?: createLodgeWarrants986.prisonTelephoneNumber ?: "",
        prisonName,
        imprisonmentInDefaultOfFine.appearanceDate,
        "3.23",
      ),
    )

    return documentGeneratorService.renderDocument(lodgeWarrants986)
  }
}
