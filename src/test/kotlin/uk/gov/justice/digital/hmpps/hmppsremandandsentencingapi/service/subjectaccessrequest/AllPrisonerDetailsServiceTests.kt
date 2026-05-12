package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.subjectaccessrequest

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.alldata.Prisoner
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.subjectaccessrequest.alldata.CourtCaseSarRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.subjectaccessrequest.alldata.ImmigrationDetentionSarRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.subjectaccessrequest.alldata.RecallSarRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.CourtRegisterService
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.PersonService
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class AllPrisonerDetailsServiceTests {

  @MockK
  private lateinit var courtCaseSarRepository: CourtCaseSarRepository

  @MockK
  private lateinit var recallSarRepository: RecallSarRepository

  @MockK
  private lateinit var immigrationDetentionSarRepository: ImmigrationDetentionSarRepository

  @MockK
  private lateinit var personService: PersonService

  @MockK
  private lateinit var courtRegisterService: CourtRegisterService

  @InjectMockKs(overrideValues = true) // 3. Use @InjectMockKs for the service
  private lateinit var sut: AllPrisonerDetailsService

  @Test
  fun `should return all Court Case Prisoner Details`() {
    arrange("44959")

    // Act
    val prisoner = sut.getPrisonerDetails("44959") as Prisoner

    assertThat(prisoner.courtCases?.count()).isEqualTo(1)
    assertThat(prisoner.courtCases?.first()).isEqualTo(ExpectedResponseData.expectedBaseCourtCaseDetails())
  }

  @Test
  fun `should return all Court Case Prisoner Details based on FROM date`() {
    arrange("44959")

    // Act
    val prisoner = sut.getPrisonerDetails("44959", LocalDate.of(2026, 1, 4)) as Prisoner

    assertThat(prisoner.courtCases?.count()).isEqualTo(1)
    assertThat(prisoner.courtCases?.first()).isEqualTo(ExpectedResponseData.expectedBaseCourtCaseDetails())
  }

  @Test
  fun `should return single Court Case Prisoner Details based on FROM date`() {
    arrange("44959")

    // Act
    val prisoner = sut.getPrisonerDetails("44959", LocalDate.of(2026, 2, 28)) as Prisoner

    val expected = ExpectedResponseData.expectedBaseCourtCaseDetails().copy(
      appearances = listOf(ExpectedResponseData.expectedCourtAppearances()[1]),
    )
    assertThat(prisoner.courtCases?.count()).isEqualTo(1)
    assertThat(prisoner.courtCases?.first()).isEqualTo(expected)
  }

  @Test
  fun `should return no Court Cases When Prisoner Details based on FROM date in future`() {
    arrange("4492234")

    // Act
    val prisoner = sut.getPrisonerDetails("4492234", LocalDate.of(2026, 7, 28)) as Prisoner

    assertThat(prisoner.courtCases?.count()).isEqualTo(0)
  }

  @Test
  fun `should return single Court Appearance Court Case Prisoner Details up until TO date`() {
    arrange("22959")

    // Act
    val prisoner = sut.getPrisonerDetails("22959", to = LocalDate.of(2026, 2, 28)) as Prisoner

    val expected = ExpectedResponseData.expectedBaseCourtCaseDetails().copy(
      latestCourtAppearance = null,
      appearances = listOf(ExpectedResponseData.expectedCourtAppearances()[0]),
    )
    assertThat(prisoner.courtCases?.count()).isEqualTo(1)
    assertThat(prisoner.courtCases?.first()).isEqualTo(expected)
  }

  @Test
  fun `should return all Court Case Prisoner Details up until TO date`() {
    arrange("22959")

    // Act
    val prisoner = sut.getPrisonerDetails("22959", to = LocalDate.of(2026, 3, 28)) as Prisoner

    assertThat(prisoner.courtCases?.count()).isEqualTo(1)
    assertThat(prisoner.courtCases?.first()).isEqualTo(ExpectedResponseData.expectedBaseCourtCaseDetails())
  }

  @Test
  fun `should return all Court Case Prisoner Details FROM date TO date`() {
    arrange("22959")

    // Act
    val prisoner = sut.getPrisonerDetails("22959", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 28)) as Prisoner

    val expected = ExpectedResponseData.expectedBaseCourtCaseDetails().copy(
      latestCourtAppearance = ExpectedResponseData.expectedCourtAppearances()[1],
      appearances = listOf(ExpectedResponseData.expectedCourtAppearances()[1]),
    )
    assertThat(prisoner.courtCases?.count()).isEqualTo(1)
    assertThat(prisoner.courtCases?.first()).isEqualTo(expected)
  }

  @Test
  fun `should return single Court Appearance Court Case Prisoner Details FROM until TO date`() {
    arrange("22959")

    // Act
    val prisoner = sut.getPrisonerDetails("22959", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 28)) as Prisoner

    assertThat(prisoner.courtCases?.count()).isEqualTo(1)
    assertThat(prisoner.courtCases?.first()).isEqualTo(ExpectedResponseData.expectedBaseCourtCaseDetails())
  }

  @Test
  fun `should return no Court Cases When Prisoner Details based on TO until FROM date in future`() {
    arrange("22959")

    // Act
    val prisoner = sut.getPrisonerDetails("22959", LocalDate.of(2026, 4, 1), LocalDate.of(2026, 5, 1)) as Prisoner

    assertThat(prisoner.courtCases?.count()).isEqualTo(0)
  }

  @Test
  fun `should return all Recall Prisoner Details`() {
    arrange("5534")

    // Act
    val prisoner = sut.getPrisonerDetails("5534") as Prisoner

    assertThat(prisoner.recalls?.count()).isEqualTo(2)
    assertThat(prisoner.recalls?.first()).isEqualTo(ExpectedResponseData.expectedBaseRecallDetails(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 7, 2)))
  }

  @Test
  fun `should return all Recall Prisoner Details FROM date`() {
    arrange("5534")

    // Act
    val prisoner = sut.getPrisonerDetails("5534", LocalDate.of(2026, 8, 1)) as Prisoner

    assertThat(prisoner.recalls?.count()).isEqualTo(1)
    assertThat(prisoner.recalls?.first()).isEqualTo(ExpectedResponseData.expectedBaseRecallDetails(LocalDate.of(2026, 8, 5), LocalDate.of(2026, 9, 1)))
  }

  @Test
  fun `should return all Recall Prisoner Details TO date`() {
    arrange("5534")

    // Act
    val prisoner = sut.getPrisonerDetails("5534", to = LocalDate.of(2026, 8, 1)) as Prisoner

    assertThat(prisoner.recalls?.count()).isEqualTo(1)
    assertThat(prisoner.recalls?.first()).isEqualTo(ExpectedResponseData.expectedBaseRecallDetails(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 7, 2)))
  }

  @Test
  fun `should return all Recall Prisoner Details FROM date TO date`() {
    arrange("5534")

    // Act
    val prisoner = sut.getPrisonerDetails("5534", from = LocalDate.of(2026, 5, 1), to = LocalDate.of(2026, 12, 31)) as Prisoner

    assertThat(prisoner.recalls?.count()).isEqualTo(2)
    assertThat(prisoner.recalls?.first()).isEqualTo(ExpectedResponseData.expectedBaseRecallDetails(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 7, 2)))
  }

  @Test
  fun `should return single Recall Prisoner Details FROM date TO date`() {
    arrange("5534")

    // Act
    val prisoner = sut.getPrisonerDetails("5534", from = LocalDate.of(2026, 5, 1), to = LocalDate.of(2026, 8, 1)) as Prisoner

    assertThat(prisoner.recalls?.count()).isEqualTo(1)
    assertThat(prisoner.recalls?.first()).isEqualTo(ExpectedResponseData.expectedBaseRecallDetails(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 7, 2)))
  }

  @Test
  fun `should return all Immigration Detention Prisoner Details`() {
    arrange("5574")

    // Act
    val prisoner = sut.getPrisonerDetails("5574") as Prisoner

    assertThat(prisoner.immigrationDetentions?.count()).isEqualTo(2)
    assertThat(prisoner.immigrationDetentions?.first()).isEqualTo(ExpectedResponseData.expectedBaseImmigrationDetentionDetails(LocalDate.of(2026, 6, 1)))
  }

  @Test
  fun `should return all Immigration Detention Prisoner Details FROM date`() {
    arrange("5574")

    // Act
    val prisoner = sut.getPrisonerDetails("5574", from = LocalDate.of(2026, 1, 1)) as Prisoner

    assertThat(prisoner.immigrationDetentions?.count()).isEqualTo(2)
    assertThat(prisoner.immigrationDetentions?.first()).isEqualTo(ExpectedResponseData.expectedBaseImmigrationDetentionDetails(LocalDate.of(2026, 6, 1)))
  }

  @Test
  fun `should return single Immigration Detention Prisoner Details FROM date`() {
    arrange("5574")

    // Act
    val prisoner = sut.getPrisonerDetails("5574", from = LocalDate.of(2026, 6, 5)) as Prisoner

    assertThat(prisoner.immigrationDetentions?.count()).isEqualTo(1)
    assertThat(prisoner.immigrationDetentions?.first()).isEqualTo(ExpectedResponseData.expectedBaseImmigrationDetentionDetails(LocalDate.of(2026, 8, 5)))
  }

  @Test
  fun `should return all Immigration Detention Prisoner Details TO date`() {
    arrange("5574")

    // Act
    val prisoner = sut.getPrisonerDetails("5574", to = LocalDate.of(2026, 6, 5)) as Prisoner

    assertThat(prisoner.immigrationDetentions?.count()).isEqualTo(1)
    assertThat(prisoner.immigrationDetentions?.first()).isEqualTo(ExpectedResponseData.expectedBaseImmigrationDetentionDetails(LocalDate.of(2026, 6, 1)))
  }

  @Test
  fun `should return single Immigration Detention Prisoner Details FROM date TO date`() {
    arrange("5574")

    // Act
    val prisoner = sut.getPrisonerDetails("5574", from = LocalDate.of(2026, 5, 31), to = LocalDate.of(2026, 6, 5)) as Prisoner

    assertThat(prisoner.immigrationDetentions?.count()).isEqualTo(1)
    assertThat(prisoner.immigrationDetentions?.first()).isEqualTo(ExpectedResponseData.expectedBaseImmigrationDetentionDetails(LocalDate.of(2026, 6, 1)))
  }

  @Test
  fun `should return no Immigration Detention Prisoner Details FROM date TO date`() {
    arrange("5574")

    // Act
    val prisoner = sut.getPrisonerDetails("5574", from = LocalDate.of(2026, 6, 2), to = LocalDate.of(2026, 6, 5)) as Prisoner

    assertThat(prisoner.immigrationDetentions?.count()).isEqualTo(0)
  }

  @Test
  fun `should return all Immigration Detention Prisoner Details FROM date TO date`() {
    arrange("5574")

    // Act
    val prisoner = sut.getPrisonerDetails("5574", from = LocalDate.of(2026, 6, 1), to = LocalDate.of(2026, 8, 5)) as Prisoner

    assertThat(prisoner.immigrationDetentions?.count()).isEqualTo(2)
    assertThat(prisoner.immigrationDetentions?.first()).isEqualTo(ExpectedResponseData.expectedBaseImmigrationDetentionDetails(LocalDate.of(2026, 6, 1)))
  }

  @Test
  fun `should return empty when Immigration Detention, Recall, Court Case Prisoner Details not found`() {
    arrangeEmpty("5574")

    // Act
    val prisoner = sut.getPrisonerDetails("5574") as Prisoner

    assertThat(prisoner.immigrationDetentions?.count()).isEqualTo(0)
    assertThat(prisoner.recalls?.count()).isEqualTo(0)
    assertThat(prisoner.courtCases?.count()).isEqualTo(0)
    assertThat(prisoner.prisonerName).isEqualTo("John Smith")
    assertThat(prisoner.prisonerNumber).isEqualTo("5574")
  }

  @Test
  fun `should find prisoner id in at least one court case`() {
    every { courtCaseSarRepository.findByPrisonerId("44959") } returns listOf()
    every { courtCaseSarRepository.existsByPrisonerId("44959") } returns true
    every { recallSarRepository.findByPrisonerId("44959") } returns listOf()
    every { immigrationDetentionSarRepository.findByPrisonerId("44959") } returns listOf()
    every { personService.getPersonDetailsByPrisonerIdCached("44959") } returns null

    // Act
    val prisoner = sut.getPrisonerDetails("44959") as Prisoner

    assertThat(prisoner).isEqualTo(Prisoner("44959", null, listOf(), listOf(), listOf()))
  }

  @Test
  fun `should find no prisoner id with a court case`() {
    every { courtCaseSarRepository.existsByPrisonerId("44959") } returns false

    // Act
    val prisoner = sut.getPrisonerDetails("44959") as Prisoner?

    assertThat(prisoner).isEqualTo(null)
  }

  private fun arrange(prn: String) {
    every { courtCaseSarRepository.findByPrisonerId(prn) } returns listOf(
      MockedResponseData.constructBaseCourtCaseSarEntity(prn),
    )
    every { recallSarRepository.findByPrisonerId(prn) } returns listOf(
      MockedResponseData.constructBaseRecallSarEntity(
        prn,
        LocalDate.of(2026, 6, 1),
        LocalDate.of(2026, 7, 2),
      ),
      MockedResponseData.constructBaseRecallSarEntity(
        prn,
        LocalDate.of(2026, 8, 5),
        LocalDate.of(2026, 9, 1),
      ),
    )
    every { immigrationDetentionSarRepository.findByPrisonerId(prn) } returns listOf(
      MockedResponseData.constructImmigrationDetentionSarEntity(
        prn,
        LocalDate.of(2026, 6, 1),
      ),
      MockedResponseData.constructImmigrationDetentionSarEntity(
        prn,
        LocalDate.of(2026, 8, 5),
      ),
    )
    every { courtRegisterService.getCourtRegisterByCourtCodeCached(any()) } returns MockedResponseData.constructCourtRegister(
      prn,
    )
    every { personService.getPersonDetailsByPrisonerIdCached(prn) } returns MockedResponseData.constructPrisonerDetails(
      prn,
    )
    every { courtCaseSarRepository.existsByPrisonerId(prn) } returns true
  }

  private fun arrangeEmpty(prn: String) {
    every { courtCaseSarRepository.findByPrisonerId(prn) } returns listOf()
    every { recallSarRepository.findByPrisonerId(prn) } returns listOf()
    every { immigrationDetentionSarRepository.findByPrisonerId(prn) } returns listOf()
    every { personService.getPersonDetailsByPrisonerIdCached(prn) } returns MockedResponseData.constructPrisonerDetails(
      prn,
    )
    every { courtCaseSarRepository.existsByPrisonerId(prn) } returns true
  }
}
