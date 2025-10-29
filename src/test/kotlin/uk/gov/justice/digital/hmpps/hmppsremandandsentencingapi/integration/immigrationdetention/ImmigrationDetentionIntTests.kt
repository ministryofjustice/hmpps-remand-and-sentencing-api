package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.immigrationdetention

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateImmigrationDetention
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.ImmigrationDetention
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.EventSource.DPS
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ImmigrationDetentionNoLongerOfInterestType.OTHER_REASON
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ImmigrationDetentionRecordType.DEPORTATION_ORDER
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ImmigrationDetentionRecordType.IS91
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ImmigrationDetentionRecordType.NO_LONGER_OF_INTEREST
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

class ImmigrationDetentionIntTests(@Autowired private val clientRegistrationRepository: InMemoryClientRegistrationRepository) : IntegrationTestBase() {
  @Test
  fun `Create an Immigration Detention record and fetch it based on returned UUID`() {
    val immigrationDetention = CreateImmigrationDetention(
      prisonerId = "A12345B",
      immigrationDetentionRecordType = IS91,
      recordDate = LocalDate.of(2021, 1, 1),
      createdByUsername = "aUser",
      createdByPrison = "PRI",
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
  }

  @Test
  fun `Update an Immigration Detention record and fetch it based on returned UUID`() {
    val immigrationDetention = CreateImmigrationDetention(
      prisonerId = "B12345B",
      immigrationDetentionRecordType = DEPORTATION_ORDER,
      recordDate = LocalDate.of(2021, 1, 1),
      createdByUsername = "aUser",
      createdByPrison = "PRI",
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
          immigrationDetentionRecordType = DEPORTATION_ORDER,
          recordDate = LocalDate.of(2021, 1, 1),
          createdAt = ZonedDateTime.now(),
        ),
      )
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
  fun `Get all immigration detention records for a prisoner`() {
    val id1 = CreateImmigrationDetention(
      prisonerId = "B12345B",
      immigrationDetentionRecordType = DEPORTATION_ORDER,
      createdByUsername = "aUser",
      recordDate = LocalDate.of(2021, 1, 1),
      createdByPrison = "PRI",
    )
    val id1Uuid = createImmigrationDetention(id1).immigrationDetentionUuid

    val id2 = CreateImmigrationDetention(
      prisonerId = "B12345B",
      immigrationDetentionRecordType = NO_LONGER_OF_INTEREST,
      createdByUsername = "aUser",
      recordDate = LocalDate.of(2021, 1, 2),
      createdByPrison = "PRI",
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
  fun `Delete an Immigration Detention record`() {
    val immigrationDetention = CreateImmigrationDetention(
      prisonerId = "B12345B",
      immigrationDetentionRecordType = NO_LONGER_OF_INTEREST,
      recordDate = LocalDate.of(2021, 1, 1),
      createdByUsername = "aUser",
      createdByPrison = "PRI",
    )

    val immigrationDetentionResponse = createImmigrationDetention(immigrationDetention)

    deleteImmigrationDetention(immigrationDetentionResponse.immigrationDetentionUuid)

    assertThat(getImmigrationDetentionsByPrisonerId("B12345B")).isEmpty()
  }
}
