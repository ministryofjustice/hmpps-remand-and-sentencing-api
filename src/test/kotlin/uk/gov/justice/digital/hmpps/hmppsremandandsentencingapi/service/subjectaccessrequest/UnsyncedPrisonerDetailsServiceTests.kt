package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.subjectaccessrequest

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.ImmigrationDetention
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.Prisoner
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.subjectaccessrequest.ImmigrationDetentionUnsyncedSarEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ImmigrationDetentionNoLongerOfInterestType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.subjectaccessrequest.CourtCaseUnsyncedSarRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.subjectaccessrequest.ImmigrationDetentionUnsyncedSarRepository
import java.time.LocalDate

class UnsyncedPrisonerDetailsServiceTests {

  private val immigrationDetentionUnsyncedRepository = mockk<ImmigrationDetentionUnsyncedSarRepository>()
  private val courtCaseUnsyncedSarRepository = mockk<CourtCaseUnsyncedSarRepository>()

  @Test
  fun `should return all Immigration Detention Prisoner Details`() {
    every { immigrationDetentionUnsyncedRepository.findByPrisonerId("44959") } returns listOf(
      ImmigrationDetentionUnsyncedSarEntity(34, "44959", LocalDate.of(2020, 1, 1), "asdasdas", ImmigrationDetentionNoLongerOfInterestType.BRITISH_CITIZEN.toString(), ""),
      ImmigrationDetentionUnsyncedSarEntity(456, "44959", LocalDate.of(2026, 12, 19), "asdasdas", ImmigrationDetentionNoLongerOfInterestType.BRITISH_CITIZEN.toString(), "Lorem ipsum"),
    )
    every { courtCaseUnsyncedSarRepository.existsByPrisonerId("44959") } returns true
    val service = UnsyncedPrisonerDetailsService(immigrationDetentionUnsyncedRepository, courtCaseUnsyncedSarRepository)

    // Act
    val prisoner = service.getPrisonerDetails("44959")

    assertThat(prisoner as Prisoner).isNotNull()
    assertThat(prisoner.immigrationDetentions).isEqualTo(
      listOf(
        ImmigrationDetention("asdasdas", "BRITISH_CITIZEN", ""),
        ImmigrationDetention("asdasdas", "BRITISH_CITIZEN", "Lorem ipsum"),
      ),
    )
  }

  @Test
  fun `should return all Immigration Detention Prisoner Details from date`() {
    every { immigrationDetentionUnsyncedRepository.findByPrisonerId("44959") } returns listOf(
      ImmigrationDetentionUnsyncedSarEntity(34, "44959", LocalDate.of(2020, 1, 1), "124222111", ImmigrationDetentionNoLongerOfInterestType.BRITISH_CITIZEN.toString(), ""),
      ImmigrationDetentionUnsyncedSarEntity(456, "44959", LocalDate.of(2026, 12, 19), null, ImmigrationDetentionNoLongerOfInterestType.BRITISH_CITIZEN.toString(), "Lorem ipsum"),
    )
    every { courtCaseUnsyncedSarRepository.existsByPrisonerId("44959") } returns true
    val service = UnsyncedPrisonerDetailsService(immigrationDetentionUnsyncedRepository, courtCaseUnsyncedSarRepository)

    // Act
    val prisoner = service.getPrisonerDetails("44959", LocalDate.of(2020, 2, 1))

    assertThat(prisoner as Prisoner).isNotNull()
    assertThat(prisoner.immigrationDetentions).isEqualTo(
      listOf(
        ImmigrationDetention(null, "BRITISH_CITIZEN", "Lorem ipsum"),
      ),
    )
  }

  @Test
  fun `should return all Immigration Detention Prisoner Details to date`() {
    every { immigrationDetentionUnsyncedRepository.findByPrisonerId("44959") } returns listOf(
      ImmigrationDetentionUnsyncedSarEntity(34, "44959", LocalDate.of(2020, 1, 1), "124222111", ImmigrationDetentionNoLongerOfInterestType.BRITISH_CITIZEN.toString(), ""),
      ImmigrationDetentionUnsyncedSarEntity(456, "44959", LocalDate.of(2026, 12, 19), null, ImmigrationDetentionNoLongerOfInterestType.BRITISH_CITIZEN.toString(), "Lorem ipsum"),
    )
    every { courtCaseUnsyncedSarRepository.existsByPrisonerId("44959") } returns true
    val service = UnsyncedPrisonerDetailsService(immigrationDetentionUnsyncedRepository, courtCaseUnsyncedSarRepository)

    // Act
    val prisoner = service.getPrisonerDetails("44959", to = LocalDate.of(2025, 2, 1))

    assertThat(prisoner as Prisoner).isNotNull()
    assertThat(prisoner.immigrationDetentions).isEqualTo(
      listOf(
        ImmigrationDetention("124222111", "BRITISH_CITIZEN", ""),
      ),
    )
  }

  @Test
  fun `should return all Immigration Detention Prisoner Details from date to date`() {
    every { immigrationDetentionUnsyncedRepository.findByPrisonerId("44959") } returns listOf(
      ImmigrationDetentionUnsyncedSarEntity(34, "44959", LocalDate.of(2020, 1, 1), "124222111", ImmigrationDetentionNoLongerOfInterestType.BRITISH_CITIZEN.toString(), ""),
      ImmigrationDetentionUnsyncedSarEntity(456, "44959", LocalDate.of(2026, 11, 10), null, ImmigrationDetentionNoLongerOfInterestType.BRITISH_CITIZEN.toString(), "Lorem ipsum"),
      ImmigrationDetentionUnsyncedSarEntity(456, "44959", LocalDate.of(2026, 12, 19), null, ImmigrationDetentionNoLongerOfInterestType.BRITISH_CITIZEN.toString(), "dolor sit amet"),
    )
    every { courtCaseUnsyncedSarRepository.existsByPrisonerId("44959") } returns true
    val service = UnsyncedPrisonerDetailsService(immigrationDetentionUnsyncedRepository, courtCaseUnsyncedSarRepository)

    // Act
    val prisoner = service.getPrisonerDetails("44959", LocalDate.of(2020, 1, 1), to = LocalDate.of(2026, 11, 10))

    assertThat(prisoner as Prisoner).isNotNull()
    assertThat(prisoner.immigrationDetentions).isEqualTo(
      listOf(
        ImmigrationDetention("124222111", "BRITISH_CITIZEN", ""),
        ImmigrationDetention(null, "BRITISH_CITIZEN", "Lorem ipsum"),
      ),
    )
  }

  @Test
  fun `should return empty when Immigration Detention Prisoner Details not found`() {
    every { immigrationDetentionUnsyncedRepository.findByPrisonerId(eq("23456")) } returns listOf()
    every { courtCaseUnsyncedSarRepository.existsByPrisonerId(eq("23456")) } returns false
    val service = UnsyncedPrisonerDetailsService(immigrationDetentionUnsyncedRepository, courtCaseUnsyncedSarRepository)

    // Act
    val prisoner = service.getPrisonerDetails("23456")

    assertThat(prisoner).isNull()
  }

  @Test
  fun `should find prisoner id in at least one court case`() {
    every { immigrationDetentionUnsyncedRepository.findByPrisonerId("4454") } returns listOf()
    every { courtCaseUnsyncedSarRepository.existsByPrisonerId("4454") } returns true
    val service = UnsyncedPrisonerDetailsService(immigrationDetentionUnsyncedRepository, courtCaseUnsyncedSarRepository)

    // Act
    val prisoner = service.getPrisonerDetails("4454")

    assertThat((prisoner as Prisoner).prisonerNumber).isEqualTo("4454")
    assertThat(prisoner.immigrationDetentions).isEmpty()
  }
}
