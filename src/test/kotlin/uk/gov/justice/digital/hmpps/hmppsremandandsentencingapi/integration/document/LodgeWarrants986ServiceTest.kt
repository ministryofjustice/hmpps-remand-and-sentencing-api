package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.document

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.AgencyDetails
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.OffenceDetails
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.PersonPrison
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.PrisonSearchDetails
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateFineAmount
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreatePeriodLength
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.documents.CreateLodgeWarrants986
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.wiremock.AdjustmentsApiExtension
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.wiremock.CourtRegisterApiExtension
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.wiremock.ManageOffencesApiExtension
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.wiremock.PersonRecordExtension
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.wiremock.PrisonApiExtension
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.wiremock.PrisonRegisterExtension
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.wiremock.PrisonSearchExtension
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.LodgeWarrants986Service
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator.Factory.DEFAULT_PRISONER_ID
import java.io.File
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import kotlin.random.Random

class LodgeWarrants986ServiceTest : IntegrationTestBase() {

  @Autowired
  private lateinit var lodgeWarrants986Service: LodgeWarrants986Service

  private val courtAppearanceUuid = UUID.fromString("15f21679-268f-44c5-9a58-00aaa00c24f1")
  private val fineSentenceTypeUuid = UUID.fromString("c71ceefe-932b-4a69-b87c-7c1294e37cf7")

  @Test
  fun `should generate pdf from CreateLodgeWarrants986 dto with single offence`() {
    arrangeSingleOffence()
    val resp = lodgeWarrants986Service.renderDocument(CreateLodgeWarrants986(courtAppearanceUuid, "Liverpool Crown Court", "Derby Square", "Liverpool", "Merseyside", "L2 1XA", "128 555 1719"))

    val outputDir = File("build/test-generated").apply { mkdirs() }
    File(outputDir, "lodge-warrants-single-sample.pdf").writeBytes(resp.readAllBytes())
  }

  @Test
  fun `should generate pdf from CreateLodgeWarrants986 dto with multiple offences and random set of periods`() {
    arrangeMultipleOffencesAndPeriods()
    val resp = lodgeWarrants986Service.renderDocument(CreateLodgeWarrants986(courtAppearanceUuid, "Liverpool Crown Court", "Derby Square", "Liverpool", "Merseyside", "L2 1XA", "128 555 1719"))

    val outputDir = File("build/test-generated").apply { mkdirs() }
    File(outputDir, "lodge-warrants-multiple-sample.pdf").writeBytes(resp.readAllBytes())
  }

  private fun arrangeSingleOffence() {
    val firstCharge = DpsDataCreator.dpsCreateCharge(offenceCode = "AA06027", sentence = DpsDataCreator.dpsCreateSentence(fineAmount = CreateFineAmount(BigDecimal(100.0)), sentenceTypeId = fineSentenceTypeUuid), legacyData = DataCreator.chargeLegacyData(offenceDescription = "Veterinary surgeon fail to notify incorrect certification"))
    val appearance = DpsDataCreator.dpsCreateCourtAppearance(charges = listOf(firstCharge), appearanceUUID = UUID.fromString("15f21679-268f-44c5-9a58-00aaa00c24f1"), outcomeUuid = UUID.fromString("04f21679-268f-44c5-9a58-00aaa00c24e0"))
    createCourtCase(DpsDataCreator.dpsCreateCourtCase(prisonerId = DEFAULT_PRISONER_ID, appearances = listOf(appearance)))

    PrisonApiExtension.prisonApi.stubGetPrisonerDetails(DEFAULT_PRISONER_ID)
    CourtRegisterApiExtension.courtRegisterApi.stubGetCourtRegister("COURT1")
    AdjustmentsApiExtension.adjustmentsApi.stubAllowCreateAdjustments()
    PersonRecordExtension.personRecord.stubGetPersonPrison(
      DEFAULT_PRISONER_ID,
      PersonPrison(
        "Joe",
        "Bloggs",
        PersonPrison.Gender("N", "Not Known / Not Recorded"),
      ),
    )
    PrisonRegisterExtension.prisonRegister.stubGetAgencyDetails("KMI", AgencyDetails("KMI", "Kirkham (HMP)", "PRISON", true, listOf(AgencyDetails.Address("Freckleton Road", "Kirkham", "Preston", "Lancashire", "PR4 2RN")), listOf(AgencyDetails.Telephone(number = "0394839394"))))
    PrisonRegisterExtension.prisonRegister.stubGetAgencyDetails("COURT1", AgencyDetails("COURT1", "COURT (#1)", "COURT", true, listOf(AgencyDetails.Address("Court Address Line 1", "Court Address Line 2", "Court Address Town", "Court Address County", "ABC DEF")), listOf(AgencyDetails.Telephone(number = "0394839394"))))
    PrisonSearchExtension.prisonSearch.stubPrisonSearchDetails(DEFAULT_PRISONER_ID, PrisonSearchDetails("KMI", "Kirkham (HMP)"))
    ManageOffencesApiExtension.manageOffencesApi.stubManageOffences("AA06027", OffenceDetails(48437, "AA06027", "Veterinary surgeon fail to notify incorrect certification"))
  }

  private fun arrangeMultipleOffencesAndPeriods() {
    val courtCode = "COURT1"
    val appearanceDate = LocalDate.of(2014, 7, 9)
    val fineAmount = CreateFineAmount(BigDecimal("620.00"))

    val offenceCodes = listOf("CJ88001", "CA03010", "RL01043", "RL01043", "MD71530", "MD71530")

    val charges = offenceCodes.mapIndexed { index, offenceCode ->
      DpsDataCreator.dpsCreateCharge(
        offenceCode = offenceCode,
        sentence = DpsDataCreator.dpsCreateSentence(
          sentenceTypeId = fineSentenceTypeUuid,
          chargeNumber = (index + 1).toString(),
          periodLengths = randomPeriodLengths(),
          fineAmount = fineAmount,
        ),
        legacyData = DataCreator.chargeLegacyData(offenceDescription = "Offence description for $offenceCode"),
      )
    }

    val appearance = DpsDataCreator.dpsCreateCourtAppearance(
      charges = charges,
      appearanceUUID = UUID.fromString("15f21679-268f-44c5-9a58-00aaa00c24f1"),
      outcomeUuid = UUID.fromString("04f21679-268f-44c5-9a58-00aaa00c24e0"),
      courtCode = courtCode,
      appearanceDate = appearanceDate,
    )
    createCourtCase(DpsDataCreator.dpsCreateCourtCase(prisonerId = DEFAULT_PRISONER_ID, appearances = listOf(appearance)))

    PrisonApiExtension.prisonApi.stubGetPrisonerDetails(DEFAULT_PRISONER_ID)
    CourtRegisterApiExtension.courtRegisterApi.stubGetCourtRegister(courtCode)
    AdjustmentsApiExtension.adjustmentsApi.stubAllowCreateAdjustments()
    PersonRecordExtension.personRecord.stubGetPersonPrison(
      DEFAULT_PRISONER_ID,
      PersonPrison(
        "Joe",
        "Bloggs",
        PersonPrison.Gender("N", "Not Known / Not Recorded"),
      ),
    )
    PrisonSearchExtension.prisonSearch.stubPrisonSearchDetails(DEFAULT_PRISONER_ID, PrisonSearchDetails("KMI", "Kirkham (HMP)"))
    PrisonRegisterExtension.prisonRegister.stubGetAgencyDetails("KMI", AgencyDetails("KMI", "Kirkham (HMP)", "PRISON", true, listOf(AgencyDetails.Address("Freckleton Road", "Kirkham", "Preston", "Lancashire", "PR4 2RN")), listOf(AgencyDetails.Telephone(number = "0394839394"))))
    PrisonRegisterExtension.prisonRegister.stubGetAgencyDetails("COURT1", AgencyDetails("COURT1", "COURT (#1)", "COURT", true, listOf(AgencyDetails.Address("Court Address Line 1", "Court Address Line 2", "Court Address Town", "Court Address County", "ABC DEF")), listOf(AgencyDetails.Telephone(number = "0394839394"))))
    ManageOffencesApiExtension.manageOffencesApi.stubManageOffences("CJ88001", OffenceDetails(50171, "CJ88001", "Common assault"))
    ManageOffencesApiExtension.manageOffencesApi.stubManageOffences("CA03010", OffenceDetails(48437, "CA03010", "Veterinary surgeon fail to notify incorrect certification"))
    ManageOffencesApiExtension.manageOffencesApi.stubManageOffences("RL01043", OffenceDetails(64441, "RL01043", "Board a train without a valid ticket - railway bye-law"))
    ManageOffencesApiExtension.manageOffencesApi.stubManageOffences("MD71530", OffenceDetails(59967, "MD71530", "Possess a controlled drug of Class B - Cannabis / Cannabis Resin"))
  }

  private fun randomPeriodLengths(): List<CreatePeriodLength> = List(Random.nextInt(1, 5)) {
    DpsDataCreator.dpsCreatePeriodLength(
      years = Random.nextInt(1, 20),
      months = Random.nextInt(0, 12),
      weeks = Random.nextInt(0, 52),
      days = Random.nextInt(0, 6),
      periodOrder = "years,months,weeks,days",
    )
  }
}
