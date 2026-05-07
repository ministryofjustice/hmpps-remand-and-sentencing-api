package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.subjectaccessrequest

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.alldata.Prisoner
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.subjectaccessrequest.alldata.CourtCaseSarRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.subjectaccessrequest.alldata.RecallSarRepository

class AllPrisonerDetailsServiceTests {

  private val courtCaseSarRepository = mockk<CourtCaseSarRepository>()
  private val recallSarRepository = mockk<RecallSarRepository>()

  @Test
  fun `should return all Court Case Prisoner Details`() {
    every { courtCaseSarRepository.findByPrisonerId("44959") } returns listOf(
      MockedResponseData.constructBaseCourtCaseSarEntity("44959"),
    )
    every { courtCaseSarRepository.existsByPrisonerId("44959") } returns true
    val service = AllPrisonerDetailsService(courtCaseSarRepository, recallSarRepository)

    // Act
    val prisoner = service.getPrisonerDetails("44959") as Prisoner

    assertThat(prisoner.courtCases?.count()).isEqualTo(1)
    assertThat(prisoner.courtCases?.first()).isEqualTo(ExpectedResponseData.expectedBaseCourtCaseDetails())
  }

  @Test
  fun `should return all Court Case Prisoner Details from date`(): Unit = throw NotImplementedError()

  @Test
  fun `should return all Court Case Prisoner Details to date`(): Unit = throw NotImplementedError()

  @Test
  fun `should return all Court Case Prisoner Details from date to date`(): Unit = throw NotImplementedError()

  @Test
  fun `should return all Recall Prisoner Details`() {
    every { courtCaseSarRepository.findByPrisonerId("5534") } returns listOf(
      MockedResponseData.constructBaseCourtCaseSarEntity("5534"),
    )
    every { recallSarRepository.findByPrisonerId("5534") } returns listOf(
      MockedResponseData.constructBaseRecallSarEntity("5534"),
    )
    every { courtCaseSarRepository.existsByPrisonerId("5534") } returns true
    val service = AllPrisonerDetailsService(courtCaseSarRepository, recallSarRepository)

    // Act
    val prisoner = service.getPrisonerDetails("5534") as Prisoner

    assertThat(prisoner.recalls?.count()).isEqualTo(1)
    assertThat(prisoner.recalls?.first()).isEqualTo(ExpectedResponseData.expectedBaseRecallDetails())
  }

  @Test
  fun `should return all Recall Prisoner Details from date`(): Unit = throw NotImplementedError()

  @Test
  fun `should return all Recall Prisoner Details to date`(): Unit = throw NotImplementedError()

  @Test
  fun `should return all Recall Prisoner Details from date to date`(): Unit = throw NotImplementedError()

  @Test
  fun `should return all Immigration Detention Prisoner Details`(): Unit = throw NotImplementedError()

  @Test
  fun `should return all Immigration Detention Prisoner Details from date`(): Unit = throw NotImplementedError()

  @Test
  fun `should return all Immigration Detention Prisoner Details to date`(): Unit = throw NotImplementedError()

  @Test
  fun `should return all Immigration Detention Prisoner Details from date to date`(): Unit = throw NotImplementedError()

  @Test
  fun `should return empty when Immigration Detention, Recall, Court Case Prisoner Details not found`(): Unit = throw NotImplementedError()

  @Test
  fun `should find prisoner id in at least one court case`() {
    every { courtCaseSarRepository.findByPrisonerId("44959") } returns listOf()
    every { courtCaseSarRepository.existsByPrisonerId("44959") } returns true
    val service = AllPrisonerDetailsService(courtCaseSarRepository, recallSarRepository)

    // Act
    val prisoner = service.getPrisonerDetails("44959") as Prisoner

    assertThat(prisoner).isEqualTo(Prisoner("44959", null, listOf(), listOf(), listOf()))
  }

  @Test
  fun `should find no prisoner id with a court case`() {
    every { courtCaseSarRepository.existsByPrisonerId("44959") } returns false
    val service = AllPrisonerDetailsService(courtCaseSarRepository, recallSarRepository)

    // Act
    val prisoner = service.getPrisonerDetails("44959") as Prisoner?

    assertThat(prisoner).isEqualTo(null)
  }
}
