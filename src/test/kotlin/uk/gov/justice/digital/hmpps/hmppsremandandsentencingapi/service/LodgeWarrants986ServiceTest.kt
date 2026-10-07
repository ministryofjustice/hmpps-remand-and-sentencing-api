package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.AgencyDetails
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.CourtRegister
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.OffenceDetails
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.PersonPrison
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.PrisonSearchDetails
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.documents.CreateLodgeWarrants986
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.projection.ImprisonmentInDefaultOfFine
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.SentenceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document.DocumentGeneratorService
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document.LodgeWarrants986
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document.dto.PeriodLength
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.externalapi.CourtRegisterService
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.externalapi.ManageOffencesService
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.externalapi.PersonRecordService
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.externalapi.PrisonRegisterService
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.externalapi.PrisonSearchService
import java.io.ByteArrayInputStream
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

class LodgeWarrants986ServiceTest {

  private val sentenceRepository = mockk<SentenceRepository>()
  private val documentGeneratorService = mockk<DocumentGeneratorService>()
  private val courtRegisterService = mockk<CourtRegisterService>()
  private val prisonRegisterService = mockk<PrisonRegisterService>()
  private val manageOffencesService = mockk<ManageOffencesService>()
  private val personRecordService = mockk<PersonRecordService>()
  private val prisonSearchService = mockk<PrisonSearchService>()

  private val lodgeWarrants986Service = LodgeWarrants986Service(
    sentenceRepository,
    documentGeneratorService,
    courtRegisterService,
    prisonRegisterService,
    manageOffencesService,
    personRecordService,
    prisonSearchService,
  )

  private val prisonerId = "A1234BC"
  private val courtAppearanceId = UUID.fromString("15f21679-268f-44c5-9a58-00aaa00c24f1")

  private fun createLodgeWarrants986() = CreateLodgeWarrants986(
    courtAppearanceId,
    "Court Premise",
    "Court Street",
    "Court Town",
    "Court County",
    "AB1 2CD",
    "01234 567890",
  )

  @BeforeEach
  fun setUp() {
    every { documentGeneratorService.renderDocument(any()) } returns ByteArrayInputStream(ByteArray(0))
  }

  @Test
  fun `should throw when no imprisonment in default of fine sentences are found`() {
    every { sentenceRepository.findCourtCodesForImprisonmentInDefaultOfFine(courtAppearanceId) } returns emptyList()

    assertThatThrownBy { lodgeWarrants986Service.renderDocument(createLodgeWarrants986()) }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessageContaining("No imprisonment in default of fine sentences found for court appearance 15f21679-268f-44c5-9a58-00aaa00c24f1")
  }

  @Test
  fun `should render document using court and prison register details`() {
    val imprisonmentInDefaultOfFine = ImprisonmentInDefaultOfFine(
      prisonerId = prisonerId,
      courtCode = "COURT1",
      courtAppearanceId = 12,
      appearanceDate = LocalDate.of(2024, 1, 15),
      fineAmount = BigDecimal("100.00"),
      offenceCode = "AA06027",
      sentenceId = 1,
      days = 1,
      weeks = 2,
      months = 3,
      years = 4,
      periodOrder = "years,months,weeks,days",
    )

    every { sentenceRepository.findCourtCodesForImprisonmentInDefaultOfFine(courtAppearanceId) } returns listOf(imprisonmentInDefaultOfFine)
    every { courtRegisterService.getCourtRegisterByCourtCodeCached("COURT1") } returns CourtRegister("COURT1", "Liverpool Crown Court", "Liverpool Crown Court Description")
    every { personRecordService.getPersonPrison(prisonerId) } returns PersonPrison("Joe", "Bloggs", PersonPrison.Gender("N", "Not Known / Not Recorded"))
    every { prisonSearchService.getPrisoner(prisonerId) } returns PrisonSearchDetails("KMI", "Kirkham (HMP)")
    every { prisonRegisterService.getAgencyDetails("COURT1") } returns AgencyDetails(
      "COURT1",
      "COURT (#1)",
      "COURT",
      true,
      listOf(AgencyDetails.Address("Court Address Line 1", "Court Address Line 2", "Court Address Town", "Court Address County", "ABC DEF")),
      listOf(AgencyDetails.Telephone("0394839394")),
    )
    every { prisonRegisterService.getAgencyDetails("KMI") } returns AgencyDetails(
      "KMI",
      "Kirkham (HMP)",
      "PRISON",
      true,
      listOf(AgencyDetails.Address("Freckleton Road", "Kirkham", "Preston", "Lancashire", "PR4 2RN")),
      listOf(AgencyDetails.Telephone("0123456789")),
    )
    every { manageOffencesService.getOffenceDetails("AA06027") } returns OffenceDetails(48437, "AA06027", "Veterinary surgeon fail to notify incorrect certification")

    val documentSlot = slot<LodgeWarrants986>()
    every { documentGeneratorService.renderDocument(capture(documentSlot)) } returns ByteArrayInputStream(ByteArray(0))

    lodgeWarrants986Service.renderDocument(createLodgeWarrants986())

    val data = documentSlot.captured.data
    assertThat(data.name).isEqualTo("Joe Bloggs")
    assertThat(data.nomsNumber).isEqualTo(prisonerId)
    assertThat(data.prisonName).isEqualTo("Kirkham (HMP)")
    assertThat(data.telephoneNumber).isEqualTo("0123456789")
    assertThat(data.sentenceDate).isEqualTo(LocalDate.of(2024, 1, 15))
    assertThat(data.version).isEqualTo("3.23")
    assertThat(data.court.name).isEqualTo("Liverpool Crown Court")
    assertThat(data.court.premise).isEqualTo("Court Address Line 1")
    assertThat(data.court.street).isEqualTo("Court Address Line 2")
    assertThat(data.court.town).isEqualTo("Court Address Town")
    assertThat(data.court.county).isEqualTo("Court Address County")
    assertThat(data.court.postalCode).isEqualTo("ABC DEF")
    assertThat(data.sentences).hasSize(1)
    assertThat(data.sentences.first().offence).isEqualTo("Veterinary surgeon fail to notify incorrect certification")
    assertThat(data.sentences.first().fineAmount).isEqualTo(100.0)
    assertThat(data.sentences.first().termLengths).hasSize(1)
    assertThat(data.totalFineAmount()).isEqualTo(100.0)

    verify { documentGeneratorService.renderDocument(any()) }
  }

  @Test
  fun `should fall back to request values and offence code when external apis nothing`() {
    val imprisonmentInDefaultOfFine = ImprisonmentInDefaultOfFine(
      prisonerId = prisonerId,
      courtCode = "COURT1",
      courtAppearanceId = 21,
      appearanceDate = LocalDate.of(2024, 1, 15),
      fineAmount = null,
      offenceCode = "AA06027",
      sentenceId = 1,
      days = null,
      weeks = null,
      months = null,
      years = null,
      periodOrder = null,
    )

    every { sentenceRepository.findCourtCodesForImprisonmentInDefaultOfFine(courtAppearanceId) } returns listOf(imprisonmentInDefaultOfFine)
    every { courtRegisterService.getCourtRegisterByCourtCodeCached("COURT1") } returns null
    every { personRecordService.getPersonPrison(prisonerId) } returns null
    every { prisonSearchService.getPrisoner(prisonerId) } returns null
    every { manageOffencesService.getOffenceDetails("AA06027") } returns null

    val documentSlot = slot<LodgeWarrants986>()
    every { documentGeneratorService.renderDocument(capture(documentSlot)) } returns ByteArrayInputStream(ByteArray(0))

    val createLodgeWarrants986 = createLodgeWarrants986()
    lodgeWarrants986Service.renderDocument(createLodgeWarrants986)

    val data = documentSlot.captured.data
    assertThat(data.name).isEqualTo("A1234BC")
    assertThat(data.prisonName).isNull()
    assertThat(data.telephoneNumber).isEqualTo("01234 567890")
    assertThat(data.court.name).isNull()
    assertThat(data.court.premise).isEqualTo("Court Premise")
    assertThat(data.court.street).isEqualTo("Court Street")
    assertThat(data.court.town).isEqualTo("Court Town")
    assertThat(data.court.county).isEqualTo("Court County")
    assertThat(data.court.postalCode).isEqualTo("AB1 2CD")
    assertThat(data.sentences.first().offence).isEqualTo("Offence Code (AA06027)")
    assertThat(data.sentences.first().fineAmount).isEqualTo(0.0)
    assertThat(data.sentences.first().termLengths).containsExactly(PeriodLength())
  }
}
