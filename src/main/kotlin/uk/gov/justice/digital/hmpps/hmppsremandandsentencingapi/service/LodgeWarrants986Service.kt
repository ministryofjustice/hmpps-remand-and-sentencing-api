package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.CourtRegisterApiClient
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.ManageOffencesApiClient
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.PersonRecordClient
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.PrisonRegisterClient
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.documents.CreateLodgeWarrants986
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.SentenceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document.DocumentGeneratorService
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document.LodgeWarrants986
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document.dto.PeriodLength
import java.io.InputStream
import java.time.LocalDate

@Service
class LodgeWarrants986Service(
  private val sentenceRepository: SentenceRepository,
  private val documentGeneratorService: DocumentGeneratorService,
  private val courtRegisterApiClient: CourtRegisterApiClient,
  private val personRecordClient: PersonRecordClient,
  private val prisonRegisterClient: PrisonRegisterClient,
  private val manageOffencesApiClient: ManageOffencesApiClient,
) {

  fun renderDocument(createLodgeWarrants986: CreateLodgeWarrants986): InputStream {
    val imprisonmentInDefaultOfFineList = sentenceRepository.findCourtCodesForImprisonmentInDefaultOfFine(createLodgeWarrants986.prisonerId)
    require(imprisonmentInDefaultOfFineList.isNotEmpty()) {
      "No imprisonment in default of fine sentences found for prisoner ${createLodgeWarrants986.prisonerId}"
    }

    val imprisonmentInDefaultOfFine = imprisonmentInDefaultOfFineList.first()
    val courtRegister = courtRegisterApiClient.getCourtRegister(imprisonmentInDefaultOfFine.courtCode)
    val personRecord = personRecordClient.getPersonPrison(createLodgeWarrants986.prisonerId)
    val courtName = courtRegister?.courtName.orEmpty()
    val name = "${personRecord.firstName} ${personRecord.lastName}"
    val prisonName = prisonRegisterClient.getPrisonDetails(createLodgeWarrants986.prisonerId).prisonName

    val sentenceList = imprisonmentInDefaultOfFineList.groupBy { it.sentenceId }.map { (_, sentenceList) ->
      val periodLengths = sentenceList.map {
        PeriodLength(
          it.years ?: 0,
          it.months ?: 0,
          it.weeks ?: 0,
          it.days ?: 0,
          it.periodOrder ?: "years,months,weeks,days",
        )
      }
      val sentence = sentenceList.first()
      LodgeWarrants986.Sentence(
        periodLengths,
        manageOffencesApiClient.getOffenceDetails(sentence.offenceCode).description,
        sentence.fineAmount?.toDouble() ?: 0.0,
      )
    }

    val lodgeWarrants986 = LodgeWarrants986(
      LodgeWarrants986.Data(
        name,
        createLodgeWarrants986.prisonerId,
        LodgeWarrants986.Court(
          courtName,
          createLodgeWarrants986.courtPremise,
          createLodgeWarrants986.courtStreet,
          createLodgeWarrants986.courtTown,
          createLodgeWarrants986.courtCounty,
          createLodgeWarrants986.courtPostalCode,
        ),
        LocalDate.now(),
        sentenceList,
        createLodgeWarrants986.prisonTelephoneNumber,
        prisonName,
        imprisonmentInDefaultOfFine.appearanceDate,
        "3.23",
      ),
    )

    return documentGeneratorService.renderDocument(lodgeWarrants986)
  }
}
