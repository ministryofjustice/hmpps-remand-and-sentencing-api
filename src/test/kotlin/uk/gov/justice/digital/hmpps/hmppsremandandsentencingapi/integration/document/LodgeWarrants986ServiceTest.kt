package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.document

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.OffenceDetails
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.PersonPrison
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.PrisonDetails
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

  @Test
  fun `should generate pdf from CreateLodgeWarrants986 dto with single offence`() {
    arrangeSingleOffence()
    val resp = lodgeWarrants986Service.renderDocument(CreateLodgeWarrants986(DEFAULT_PRISONER_ID, "Liverpool Crown Court", "The Queen Elizabeth II Law Courts", "Derby Square", "Liverpool", "Merseyside", "L2 1XA", "128 555 1719"))

    val outputDir = File("build/test-generated").apply { mkdirs() }
    File(outputDir, "lodge-warrants-single-sample.pdf").writeBytes(resp.readAllBytes())
  }

  @Test
  fun `should generate pdf from CreateLodgeWarrants986 dto with multiple offences and random set of periods`() {
    arrangeMultipleOffencesAndPeriods()
    val resp = lodgeWarrants986Service.renderDocument(CreateLodgeWarrants986(DEFAULT_PRISONER_ID, "Liverpool Crown Court", "The Queen Elizabeth II Law Courts", "Derby Square", "Liverpool", "Merseyside", "L2 1XA", "128 555 1719"))

    val outputDir = File("build/test-generated").apply { mkdirs() }
    File(outputDir, "lodge-warrants-multiple-sample.pdf").writeBytes(resp.readAllBytes())
  }

  private fun arrangeSingleOffence() {
    val firstCharge = DpsDataCreator.dpsCreateCharge(offenceCode = "AA06027", sentence = DpsDataCreator.dpsCreateSentence(fineAmount = CreateFineAmount(BigDecimal(100.0))), legacyData = DataCreator.chargeLegacyData(offenceDescription = "Veterinary surgeon fail to notify incorrect certification"))
    val appearance = DpsDataCreator.dpsCreateCourtAppearance(charges = listOf(firstCharge), outcomeUuid = UUID.fromString("04f21679-268f-44c5-9a58-00aaa00c24e0"))
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
    PrisonRegisterExtension.prisonRegister.stubGetPrisonDetails(DEFAULT_PRISONER_ID, PrisonDetails("KMI", "Kirkham (HMP)", true))
    ManageOffencesApiExtension.manageOffencesApi.stubManageOffences("AA06027", OffenceDetails(48437, "AA06027", "Veterinary surgeon fail to notify incorrect certification"))
  }

  private fun arrangeMultipleOffencesAndPeriods() {
    val courtCode = "HNDNMC"
    val appearanceDate = LocalDate.of(2014, 7, 9)
    val fineAmount = CreateFineAmount(BigDecimal("620.00"))

    val offenceCodes = listOf("CJ88001", "CA03010", "RL01043", "RL01043", "MD71530", "MD71530")

    val charges = offenceCodes.mapIndexed { index, offenceCode ->
      DpsDataCreator.dpsCreateCharge(
        offenceCode = offenceCode,
        sentence = DpsDataCreator.dpsCreateSentence(
          chargeNumber = (index + 1).toString(),
          periodLengths = randomPeriodLengths(),
          fineAmount = fineAmount,
        ),
        legacyData = DataCreator.chargeLegacyData(offenceDescription = "Offence description for $offenceCode"),
      )
    }

    val appearance = DpsDataCreator.dpsCreateCourtAppearance(
      charges = charges,
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
    PrisonRegisterExtension.prisonRegister.stubGetPrisonDetails(DEFAULT_PRISONER_ID, PrisonDetails("KMI", "Kirkham (HMP)", true))
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
