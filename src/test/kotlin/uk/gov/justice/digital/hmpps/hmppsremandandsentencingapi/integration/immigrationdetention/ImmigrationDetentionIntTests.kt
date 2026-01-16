package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.immigrationdetention

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateImmigrationDetention
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.ImmigrationDetention
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.EventSource
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.EventSource.DPS
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.CourtAppearanceEntityStatus.IMMIGRATION_APPEARANCE
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ImmigrationDetentionNoLongerOfInterestType.OTHER_REASON
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ImmigrationDetentionRecordType.DEPORTATION_ORDER
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ImmigrationDetentionRecordType.IS91
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ImmigrationDetentionRecordType.NO_LONGER_OF_INTEREST
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.CourtAppearanceService
import java.lang.Thread.sleep
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

class ImmigrationDetentionIntTests(@Autowired private val courtAppearanceService: CourtAppearanceService) : IntegrationTestBase() {
  @Test
  fun `Create an Immigration Detention record and fetch it based on returned UUID also check the events are emitted`() {
    val immigrationDetention = CreateImmigrationDetention(
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
      .ignoringFields("createdAt")
      .isEqualTo(
        ImmigrationDetention(
          immigrationDetentionUuid = immigrationDetentionResponse.immigrationDetentionUuid,
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

    val immigrationDetention2 = CreateImmigrationDetention(
      prisonerId = "A12345B",
      immigrationDetentionRecordType = NO_LONGER_OF_INTEREST,
      recordDate = LocalDate.of(2021, 1, 1),
      createdByUsername = "aUser",
      createdByPrison = "PRI",
      appearanceOutcomeUuid = IMMIGRATION_NO_LONGER_OF_INTEREST_UUID,
    )

    createImmigrationDetention(immigrationDetention2)

    messages = getMessages(2)

    assertThat(messages).hasSize(2).extracting<String> { it.eventType }
      .contains("court-appearance.inserted", "charge.inserted")

    val courtCase = courtCaseRepository.findAllByPrisonerId("A12345B").firstOrNull()

    val appearances = courtAppearanceRepository.findAllByCourtCaseCaseUniqueIdentifierAndStatusId(
      courtCase?.caseUniqueIdentifier.toString(),
      IMMIGRATION_APPEARANCE,
    )

    appearances.forEach { assertThat(it.courtCode).isEqualTo("IMM") }
  }

  @Test
  fun `Update an Immigration Detention record and fetch it based on returned UUID`() {
    val immigrationDetention = CreateImmigrationDetention(
      prisonerId = "B12345B",
      immigrationDetentionRecordType = DEPORTATION_ORDER,
      recordDate = LocalDate.of(2021, 1, 1),
      createdByUsername = "aUser",
      createdByPrison = "PRI",
      appearanceOutcomeUuid = IMMIGRATION_DECISION_TO_DEPORT_UUID,
    )

    val uuid = UUID.randomUUID()
    val createResponse = updateImmigrationDetention(immigrationDetention, uuid)
    val actualImmigrationDetention =
      getImmigrationDetentionByUUID(createResponse.immigrationDetentionUuid)

    var messages = getMessages(3)

    assertThat(messages).hasSize(3).extracting<String> { it.eventType }
      .contains("court-appearance.inserted", "charge.inserted", "court-case.inserted")

    purgeQueues()

    assertThat(actualImmigrationDetention).usingRecursiveComparison()
      .ignoringFields("createdAt")
      .isEqualTo(
        ImmigrationDetention(
          immigrationDetentionUuid = createResponse.immigrationDetentionUuid,
          prisonerId = "B12345B",
          immigrationDetentionRecordType = DEPORTATION_ORDER,
          recordDate = LocalDate.of(2021, 1, 1),
          createdAt = ZonedDateTime.now(),
        ),
      )

    immigrationDetention.immigrationDetentionRecordType = NO_LONGER_OF_INTEREST
    immigrationDetention.recordDate = LocalDate.of(2021, 2, 1)
    val updateResponse = updateImmigrationDetention(immigrationDetention, uuid)

    val historicImmigrationDetention =
      immigrationDetentionHistoryRepository.findByImmigrationDetentionUuid(updateResponse.immigrationDetentionUuid)
    assertThat(historicImmigrationDetention).hasSize(1)
    assertThat(historicImmigrationDetention[0].immigrationDetentionRecordType).isEqualTo(DEPORTATION_ORDER)
    assertThat(historicImmigrationDetention[0].historyCreatedAt).isNotNull()

    val courtCase = courtCaseRepository.findAllByPrisonerId("B12345B").firstOrNull()

    val appearances = courtAppearanceRepository.findAllByCourtCaseCaseUniqueIdentifierAndStatusId(
      courtCase?.caseUniqueIdentifier.toString(),
      IMMIGRATION_APPEARANCE,
    )

    assertThat(appearances[0].appearanceDate).isEqualTo(LocalDate.of(2021, 2, 1))

    messages = getMessages(2)

    assertThat(messages).hasSize(2).extracting<String> { it.eventType }
      .contains("court-appearance.updated", "charge.updated")

    purgeQueues()
  }

  @Test
  fun `Update an Immigration Detention record to test other fields`() {
    val immigrationDetention = CreateImmigrationDetention(
      prisonerId = "B12345B",
      immigrationDetentionRecordType = NO_LONGER_OF_INTEREST,
      noLongerOfInterestReason = OTHER_REASON,
      noLongerOfInterestComment = "A Comment",
      recordDate = LocalDate.of(2021, 1, 1),
      createdByUsername = "aUser",
      createdByPrison = "PRI",
      appearanceOutcomeUuid = IMMIGRATION_NO_LONGER_OF_INTEREST_UUID,
    )

    val uuid = UUID.randomUUID()
    val immigrationDetentionResponse = updateImmigrationDetention(immigrationDetention, uuid)
    val actualImmigrationDetention =
      getImmigrationDetentionByUUID(immigrationDetentionResponse.immigrationDetentionUuid)

    assertThat(actualImmigrationDetention).usingRecursiveComparison()
      .ignoringFields("createdAt")
      .isEqualTo(
        ImmigrationDetention(
          immigrationDetentionUuid = immigrationDetentionResponse.immigrationDetentionUuid,
          prisonerId = "B12345B",
          immigrationDetentionRecordType = NO_LONGER_OF_INTEREST,
          noLongerOfInterestReason = OTHER_REASON,
          noLongerOfInterestComment = "A Comment",
          recordDate = LocalDate.of(2021, 1, 1),
          source = DPS,
          createdAt = ZonedDateTime.now(),
        ),
      )
  }

  @Test
  fun `Get all immigration detention records for a prisoner without NOMIS records`() {
    val id1 = CreateImmigrationDetention(
      prisonerId = "B12345B",
      immigrationDetentionRecordType = DEPORTATION_ORDER,
      createdByUsername = "aUser",
      recordDate = LocalDate.of(2021, 1, 1),
      createdByPrison = "PRI",
      appearanceOutcomeUuid = IMMIGRATION_DECISION_TO_DEPORT_UUID,
    )
    val id1Uuid = createImmigrationDetention(id1).immigrationDetentionUuid

    val id2 = CreateImmigrationDetention(
      prisonerId = "B12345B",
      immigrationDetentionRecordType = NO_LONGER_OF_INTEREST,
      createdByUsername = "aUser",
      recordDate = LocalDate.of(2021, 1, 2),
      createdByPrison = "PRI",
      appearanceOutcomeUuid = IMMIGRATION_NO_LONGER_OF_INTEREST_UUID,
    )
    val id2Uuid = createImmigrationDetention(id2).immigrationDetentionUuid

    val immigrationDetentionRecords = getImmigrationDetentionsByPrisonerId("B12345B")

    assertThat(immigrationDetentionRecords)
      .usingRecursiveComparison()
      .ignoringFields("createdAt")
      .ignoringCollectionOrder()
      .isEqualTo(
        listOf(
          ImmigrationDetention(
            immigrationDetentionUuid = id1Uuid,
            prisonerId = "B12345B",
            immigrationDetentionRecordType = DEPORTATION_ORDER,
            recordDate = LocalDate.of(2021, 1, 1),
            createdAt = ZonedDateTime.now(),
          ),
          ImmigrationDetention(
            immigrationDetentionUuid = id2Uuid,
            prisonerId = "B12345B",
            immigrationDetentionRecordType = NO_LONGER_OF_INTEREST,
            recordDate = LocalDate.of(2021, 1, 2),
            createdAt = ZonedDateTime.now(),
          ),
        ),
      )
  }

  @Test
  fun `Get latest immigration detention record for a prisoner without NOMIS records`() {
    val id1 = CreateImmigrationDetention(
      prisonerId = "B12345B",
      immigrationDetentionRecordType = DEPORTATION_ORDER,
      createdByUsername = "aUser",
      recordDate = LocalDate.of(2021, 1, 1),
      createdByPrison = "PRI",
      appearanceOutcomeUuid = IMMIGRATION_DECISION_TO_DEPORT_UUID,
    )
    val id1Uuid = createImmigrationDetention(id1).immigrationDetentionUuid

    sleep(1000)

    val id2 = CreateImmigrationDetention(
      prisonerId = "B12345B",
      immigrationDetentionRecordType = NO_LONGER_OF_INTEREST,
      createdByUsername = "aUser",
      recordDate = LocalDate.of(2021, 1, 2),
      createdByPrison = "PRI",
      appearanceOutcomeUuid = IMMIGRATION_NO_LONGER_OF_INTEREST_UUID,
    )
    val id2Uuid = createImmigrationDetention(id2).immigrationDetentionUuid

    val immigrationDetentionRecords = getLatestImmigrationDetentionRecordByPrisonerId("B12345B")

    assertThat(immigrationDetentionRecords)
      .usingRecursiveComparison()
      .ignoringFields("createdAt")
      .ignoringCollectionOrder()
      .isEqualTo(
        listOf(
          ImmigrationDetention(
            immigrationDetentionUuid = id2Uuid,
            prisonerId = "B12345B",
            immigrationDetentionRecordType = NO_LONGER_OF_INTEREST,
            recordDate = LocalDate.of(2021, 1, 2),
            createdAt = ZonedDateTime.now(),
          ),
        ),
      )
  }

  @Test
  fun `Get latest immigration detention record for a prisoner with NOMIS records`() {
    val id1 = CreateImmigrationDetention(
      prisonerId = "B12345B",
      immigrationDetentionRecordType = DEPORTATION_ORDER,
      createdByUsername = "aUser",
      recordDate = LocalDate.of(2021, 1, 1),
      createdByPrison = "PRI",
      appearanceOutcomeUuid = IMMIGRATION_DECISION_TO_DEPORT_UUID,
    )
    val id1Uuid = createImmigrationDetention(id1).immigrationDetentionUuid

    sleep(1000)

    val immigrationDetentionNomisUUID = createNomisImmigrationDetentionCourtCase(prisonerId = "B12345B", "5500")

    val immigrationDetentionRecords = getLatestImmigrationDetentionRecordByPrisonerId("B12345B")

    assertThat(immigrationDetentionRecords)
      .usingRecursiveComparison()
      .ignoringFields("createdAt")
      .ignoringCollectionOrder()
      .isEqualTo(
        listOf(
          ImmigrationDetention(
            immigrationDetentionUuid = immigrationDetentionNomisUUID,
            prisonerId = "B12345B",
            immigrationDetentionRecordType = IS91,
            homeOfficeReferenceNumber = "NOMIS123",
            recordDate = LocalDate.now(),
            createdAt = ZonedDateTime.now(),
            source = EventSource.NOMIS,
          ),
        ),
      )
  }

  @Test
  fun `Get all immigration detention records for a prisoner with NOMIS records`() {
    val immigrationDetentionNomisUUID = createNomisImmigrationDetentionCourtCase(prisonerId = "B12345B", "5502")
    val id1 = CreateImmigrationDetention(
      prisonerId = "B12345B",
      immigrationDetentionRecordType = DEPORTATION_ORDER,
      createdByUsername = "aUser",
      recordDate = LocalDate.of(2021, 1, 1),
      createdByPrison = "PRI",
      appearanceOutcomeUuid = IMMIGRATION_DECISION_TO_DEPORT_UUID,
    )
    val id1Uuid = createImmigrationDetention(id1).immigrationDetentionUuid

    val id2 = CreateImmigrationDetention(
      prisonerId = "B12345B",
      immigrationDetentionRecordType = NO_LONGER_OF_INTEREST,
      createdByUsername = "aUser",
      recordDate = LocalDate.of(2021, 1, 2),
      createdByPrison = "PRI",
      appearanceOutcomeUuid = IMMIGRATION_NO_LONGER_OF_INTEREST_UUID,
    )
    val id2Uuid = createImmigrationDetention(id2).immigrationDetentionUuid

    val immigrationDetentionRecords = getImmigrationDetentionsByPrisonerId("B12345B")

    assertThat(immigrationDetentionRecords)
      .usingRecursiveComparison()
      .ignoringFields("createdAt")
      .ignoringCollectionOrder()
      .isEqualTo(
        listOf(
          ImmigrationDetention(
            immigrationDetentionUuid = id1Uuid,
            prisonerId = "B12345B",
            immigrationDetentionRecordType = DEPORTATION_ORDER,
            recordDate = LocalDate.of(2021, 1, 1),
            createdAt = ZonedDateTime.now(),
          ),
          ImmigrationDetention(
            immigrationDetentionUuid = id2Uuid,
            prisonerId = "B12345B",
            immigrationDetentionRecordType = NO_LONGER_OF_INTEREST,
            recordDate = LocalDate.of(2021, 1, 2),
            createdAt = ZonedDateTime.now(),
          ),
          ImmigrationDetention(
            immigrationDetentionUuid = immigrationDetentionNomisUUID,
            prisonerId = "B12345B",
            immigrationDetentionRecordType = DEPORTATION_ORDER,
            homeOfficeReferenceNumber = "NOMIS123",
            recordDate = LocalDate.now(),
            createdAt = ZonedDateTime.now(),
            source = EventSource.NOMIS,
          ),
        ),
      )
  }

  @Test
  fun `Delete an Immigration Detention record`() {
    val id1 = CreateImmigrationDetention(
      prisonerId = "B12345B",
      immigrationDetentionRecordType = DEPORTATION_ORDER,
      createdByUsername = "aUser",
      recordDate = LocalDate.of(2021, 1, 1),
      createdByPrison = "PRI",
      appearanceOutcomeUuid = IMMIGRATION_DECISION_TO_DEPORT_UUID,
    )
    val id2 = CreateImmigrationDetention(
      prisonerId = "B12345B",
      immigrationDetentionRecordType = NO_LONGER_OF_INTEREST,
      recordDate = LocalDate.of(2021, 1, 1),
      createdByUsername = "aUser",
      createdByPrison = "PRI",
      appearanceOutcomeUuid = IMMIGRATION_NO_LONGER_OF_INTEREST_UUID,
    )

    val id1Response = createImmigrationDetention(id1)
    val id2Response = createImmigrationDetention(id2)

    var messages = getMessages(5)

    assertThat(messages).hasSize(5).extracting<String> { it.eventType }
      .contains("court-case.inserted", "court-appearance.inserted", "charge.inserted", "court-appearance.inserted", "charge.inserted")

    purgeQueues()

    deleteImmigrationDetention(id1Response.immigrationDetentionUuid)

    messages = getMessages(3)

    assertThat(messages).hasSize(3).extracting<String> { it.eventType }
      .contains("court-appearance.deleted", "charge.deleted")

    purgeQueues()

    deleteImmigrationDetention(id2Response.immigrationDetentionUuid)

    messages = getMessages(3)

    assertThat(messages).hasSize(3).extracting<String> { it.eventType }
      .contains("court-case.deleted", "court-appearance.deleted", "charge.deleted")

    purgeQueues()

    val historicImmigrationDetention =
      immigrationDetentionHistoryRepository.findByImmigrationDetentionUuid(id1Response.immigrationDetentionUuid)
    assertThat(historicImmigrationDetention).hasSize(1)
    assertThat(historicImmigrationDetention[0].historyCreatedAt).isNotNull()

    assertThat(getImmigrationDetentionsByPrisonerId("B12345B")).isEmpty()
  }

  @Test
  fun `Get the latest Immigration Detention record`() {
    createImmigrationDetention(
      CreateImmigrationDetention(
        prisonerId = "B12345B",
        immigrationDetentionRecordType = IS91,
        recordDate = LocalDate.of(2021, 1, 1),
        createdByUsername = "aUser",
        createdByPrison = "PRI",
        appearanceOutcomeUuid = IMMIGRATION_IS91_UUID,
      ),
    )
    assertThat(getLatestImmigrationDetentionByPrisonerId("B12345B").immigrationDetentionRecordType).isEqualTo(IS91)

    sleep(1000) // Ensure createdAt timestamps differ

    createImmigrationDetention(
      CreateImmigrationDetention(
        prisonerId = "B12345B",
        immigrationDetentionRecordType = NO_LONGER_OF_INTEREST,
        recordDate = LocalDate.of(2021, 2, 1),
        createdByUsername = "aUser",
        createdByPrison = "PRI",
        appearanceOutcomeUuid = IMMIGRATION_NO_LONGER_OF_INTEREST_UUID,
      ),
    )
    assertThat(getLatestImmigrationDetentionByPrisonerId("B12345B").immigrationDetentionRecordType).isEqualTo(
      NO_LONGER_OF_INTEREST,
    )
  }

  @Test
  fun `Check behaviour when there are no records`() {
    webTestClient
      .get()
      .uri("/immigration-detention/person/NOTFOUND/latest")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_SENTENCING__IMMIGRATION_DETENTION_RW"))
      }
      .exchange()
      .expectStatus()
      .isNotFound
      .expectBody()
      .jsonPath("$.userMessage")
      .isEqualTo("not found: No immigration detention records exist for the prisoner ID: NOTFOUND")
      .jsonPath("$.developerMessage").isEqualTo("No immigration detention records exist for the prisoner ID: NOTFOUND")
  }

  companion object {
    val IMMIGRATION_DECISION_TO_DEPORT_UUID: UUID = UUID.fromString("b28afb19-dd94-4970-8071-e616b33274cb")
    val IMMIGRATION_IS91_UUID: UUID = UUID.fromString("5c670576-ffbf-4005-8d54-4aeba7bf1a22")
    val IMMIGRATION_NO_LONGER_OF_INTEREST_UUID: UUID = UUID.fromString("15524814-3238-4e4b-86a7-cda31b0221ec")
  }
}
