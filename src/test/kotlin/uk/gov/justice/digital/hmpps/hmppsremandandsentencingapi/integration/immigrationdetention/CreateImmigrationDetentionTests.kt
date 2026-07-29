package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.immigrationdetention

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.ImmigrationDetention
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.CourtAppearanceEntityStatus.IMMIGRATION_APPEARANCE
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.CourtCaseEntityStatus.INACTIVE
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ImmigrationDetentionNoLongerOfInterestType.OTHER_REASON
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ImmigrationDetentionRecordType.IS91
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ImmigrationDetentionRecordType.NO_LONGER_OF_INTEREST
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*

class CreateImmigrationDetentionTests : IntegrationTestBase() {

  @Test
  fun `Create an Immigration Detention record and fetch it based on returned UUID also check the events are emitted`() {
    val immigrationDetention = DpsDataCreator.dpsCreateImmigrationDetention(
      prisonerId = "A12345B",
      immigrationDetentionRecordType = IS91,
      recordDate = LocalDate.of(2021, 1, 1),
      createdByUsername = "aUser",
      createdByPrison = "PRI",
      appearanceOutcomeUuid = IMMIGRATION_IS91_UUID,
    )

    val immigrationDetentionResponse = createImmigrationDetention(immigrationDetention)
    val actualImmigrationDetention =
      getImmigrationDetentionByUUID(immigrationDetentionResponse.immigrationDetentionUuid)

    assertThat(actualImmigrationDetention).usingRecursiveComparison()
      .ignoringFields("createdAt", "courtAppearanceUuid")
      .isEqualTo(
        ImmigrationDetention(
          immigrationDetentionUuid = immigrationDetentionResponse.immigrationDetentionUuid,
          courtAppearanceUuid = UUID.randomUUID(),
          prisonerId = "A12345B",
          immigrationDetentionRecordType = IS91,
          recordDate = LocalDate.of(2021, 1, 1),
          createdAt = ZonedDateTime.now(),
        ),
      )

    var messages = getMessages(3)

    assertThat(messages).hasSize(3).extracting<String> { it.eventType }
      .contains("court-appearance.inserted", "charge.inserted", "court-case.inserted")

    purgeQueues()

    val immigrationDetention2 = DpsDataCreator.dpsCreateImmigrationDetention(
      prisonerId = "A12345B",
      immigrationDetentionRecordType = NO_LONGER_OF_INTEREST,
      recordDate = LocalDate.of(2021, 1, 1),
      createdByUsername = "aUser",
      createdByPrison = "PRI",
      appearanceOutcomeUuid = IMMIGRATION_NO_LONGER_OF_INTEREST_UUID,
    )

    createImmigrationDetention(immigrationDetention2)

    messages = getMessages(4)

    assertThat(messages).hasSize(4).extracting<String> { it.eventType }
      .contains("court-appearance.inserted", "charge.inserted", "court-case.inserted", "court-case.updated")

    val courtAppearances = courtAppearanceRepository.findAllByCourtCasePrisonerIdAndStatusId("A12345B", IMMIGRATION_APPEARANCE)

    courtAppearances
      .forEach { appearance ->
        assertThat(appearance.courtCode).isEqualTo("IMM")
      }
  }

  @Test
  fun `create immigration detention record based on NOMIS court appearance`() {
    val (courtAppearanceUuid) = createNomisImmigrationDetentionCourtCase(prisonerId = "B12345B", "5500")
    val immigrationDetention = DpsDataCreator.dpsCreateImmigrationDetention(
      prisonerId = "B12345B",
      immigrationDetentionRecordType = IS91,
      recordDate = LocalDate.of(2021, 1, 1),
      createdByUsername = "aUser",
      createdByPrison = "PRI",
      appearanceOutcomeUuid = IMMIGRATION_IS91_UUID,
      courtAppearanceUuid = courtAppearanceUuid,
    )

    val immigrationDetentionResponse = createImmigrationDetention(immigrationDetention)
    assertThat(immigrationDetentionResponse.courtAppearanceUuid!!).isEqualTo(courtAppearanceUuid)

    val actualImmigrationDetention =
      getImmigrationDetentionByUUID(immigrationDetentionResponse.immigrationDetentionUuid)

    assertThat(actualImmigrationDetention).usingRecursiveComparison()
      .ignoringFields("createdAt")
      .isEqualTo(
        ImmigrationDetention(
          immigrationDetentionUuid = immigrationDetentionResponse.immigrationDetentionUuid,
          courtAppearanceUuid = courtAppearanceUuid,
          prisonerId = "B12345B",
          immigrationDetentionRecordType = IS91,
          recordDate = LocalDate.of(2021, 1, 1),
          createdAt = ZonedDateTime.now(),
        ),
      )

    val messages = getMessages(2)

    assertThat(messages).hasSize(2).extracting<String> { it.eventType }
      .contains("court-appearance.updated", "charge.updated")
  }

  @Test
  fun `create no-longer-of-interest outcome deactivates linked immigration court case`() {
    val (courtAppearanceUuid) = createNomisImmigrationDetentionCourtCase(prisonerId = "B12345B", "5500")
    val noLongerOfInterest = DpsDataCreator.dpsCreateImmigrationDetention(
      prisonerId = "B12345B",
      immigrationDetentionRecordType = NO_LONGER_OF_INTEREST,
      recordDate = LocalDate.of(2021, 1, 1),
      createdByUsername = "aUser",
      createdByPrison = "PRI",
      appearanceOutcomeUuid = IMMIGRATION_NO_LONGER_OF_INTEREST_UUID,
      courtAppearanceUuid = courtAppearanceUuid,
    )

    createImmigrationDetention(noLongerOfInterest)

    val courtAppearance = courtAppearanceRepository.findByAppearanceUuid(courtAppearanceUuid)!!
    val courtCase = courtCaseRepository.findByCaseUniqueIdentifier(courtAppearance.courtCase.caseUniqueIdentifier)!!
    assertThat(courtCase.statusId).isEqualTo(INACTIVE)

    val messages = getMessages(3)
    assertThat(messages).extracting<String> { it.eventType }
      .contains("court-case.updated")
      .doesNotContain("court-case.inserted")
  }

  @Test
  fun `create no-longer-of-interest from DPS with no existing court-case, creates INACTIVE court-case`() {
    val noLongerOfInterest = DpsDataCreator.dpsCreateImmigrationDetention(
      prisonerId = "C12345D",
      immigrationDetentionRecordType = NO_LONGER_OF_INTEREST,
      recordDate = LocalDate.of(2021, 3, 1),
      createdByUsername = "aUser",
      createdByPrison = "PRI",
      appearanceOutcomeUuid = IMMIGRATION_NO_LONGER_OF_INTEREST_UUID,
    )

    val response = createImmigrationDetention(noLongerOfInterest)
    val createdRecord = getImmigrationDetentionByUUID(response.immigrationDetentionUuid)

    assertThat(createdRecord).usingRecursiveComparison()
      .ignoringFields("createdAt")
      .isEqualTo(
        ImmigrationDetention(
          immigrationDetentionUuid = response.immigrationDetentionUuid,
          courtAppearanceUuid = response.courtAppearanceUuid!!,
          prisonerId = "C12345D",
          immigrationDetentionRecordType = NO_LONGER_OF_INTEREST,
          recordDate = LocalDate.of(2021, 3, 1),
          createdAt = ZonedDateTime.now(),
        ),
      )

    val courtAppearance = courtAppearanceRepository.findByAppearanceUuid(response.courtAppearanceUuid)!!
    val courtCase = courtCaseRepository.findByCaseUniqueIdentifier(courtAppearance.courtCase.caseUniqueIdentifier)!!
    assertThat(courtCase.statusId).isEqualTo(INACTIVE)

    val messages = getMessages(3)
    assertThat(messages).extracting<String> { it.eventType }
      .contains("court-case.inserted")
      .doesNotContain("court-case.updated")
  }

  @ParameterizedTest
  @CsvSource(
    "23423.444, The Home Office Reference Number should contain only letters, numbers and '/'",
    "-2342344, The Home Office Reference Number should contain only letters, numbers and '/'",
    "B12345B@, The Home Office Reference Number should contain only letters, numbers and '/'",
    "B12345B#, The Home Office Reference Number should contain only letters, numbers and '/'",
    "B12345B%, The Home Office Reference Number should contain only letters, numbers and '/'",
    "B12345B&, The Home Office Reference Number should contain only letters, numbers and '/'",
    "1233, The Home Office Reference Number should be between 5 and 16 characters.",
    "0123456789ABCDEF1A, The Home Office Reference Number should be between 5 and 16 characters.",
    "'', The Home Office Reference Number should be between 5 and 16 characters.",
  )
  fun `Should fail to create an Immigration Detention record based on invalid home office ref no`(
    homeOfficeReferenceNumber: String?,
    error: String,
  ) {
    val immigrationDetention = DpsDataCreator.dpsCreateImmigrationDetention(
      prisonerId = "B12345BÍ",
      immigrationDetentionRecordType = NO_LONGER_OF_INTEREST,
      noLongerOfInterestReason = OTHER_REASON,
      noLongerOfInterestComment = "A Comment",
      recordDate = LocalDate.of(2021, 1, 1),
      homeOfficeReferenceNumber = homeOfficeReferenceNumber,
      createdByUsername = "aUser",
      createdByPrison = "PRI",
      appearanceOutcomeUuid = IMMIGRATION_NO_LONGER_OF_INTEREST_UUID,
    )

    // Act
    createImmigrationDetentionExchange(immigrationDetention).expectStatus()
      .isBadRequest.expectBody<ErrorResponse>().value { errorMessage ->
        assertThat(errorMessage?.userMessage).contains("homeOfficeReferenceNumber: $error")
      }
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(
    strings = [
      "23423444",
      "2342344/",
      "B123/45B",
      "BBBBB",
      "BBBBBBBBBBBBBBBC",
      "12345",
      "0123456789ABCDEF",
    ],
  )
  fun `Should succeed in creating an Immigration Detention record based on valid home office ref no`(homeOfficeReferenceNumber: String?) {
    val immigrationDetention = DpsDataCreator.dpsCreateImmigrationDetention(
      prisonerId = "B12345BÍ",
      immigrationDetentionRecordType = NO_LONGER_OF_INTEREST,
      noLongerOfInterestReason = OTHER_REASON,
      noLongerOfInterestComment = "A Comment",
      recordDate = LocalDate.of(2021, 1, 1),
      homeOfficeReferenceNumber = homeOfficeReferenceNumber,
      createdByUsername = "aUser",
      createdByPrison = "PRI",
      appearanceOutcomeUuid = IMMIGRATION_NO_LONGER_OF_INTEREST_UUID,
    )

    // Act
    createImmigrationDetentionExchange(immigrationDetention).expectStatus().isCreated
  }
}
