package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ImmigrationDetentionEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ImmigrationDetentionEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ImmigrationDetentionNoLongerOfInterestType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ImmigrationDetentionRecordType
import java.time.LocalDate

class ImmigrationDetentionEntityUpdaterTests {

  @Test
  fun `existing IS91 to deportation order keeps home office reference number`() {
    val homeOfficeReferenceNumber = "123456789"
    val is91ImmigrationRecord = immigrationDetentionEntity(homeOfficeReferenceNumber = homeOfficeReferenceNumber)
    val deportationOrder = immigrationDetentionEntity(immigrationDetentionRecordType = ImmigrationDetentionRecordType.DEPORTATION_ORDER, homeOfficeReferenceNumber = null)

    ImmigrationDetentionEntityUpdater.update(is91ImmigrationRecord, deportationOrder)
    Assertions.assertThat(is91ImmigrationRecord.homeOfficeReferenceNumber).isEqualTo(homeOfficeReferenceNumber)
  }

  @Test
  fun `existing IS91 to bail clears the home office reference number`() {
    val is91ImmigrationRecord = immigrationDetentionEntity()
    val bailRecord = immigrationDetentionEntity(immigrationDetentionRecordType = ImmigrationDetentionRecordType.IMMIGRATION_BAIL, homeOfficeReferenceNumber = null)
    ImmigrationDetentionEntityUpdater.update(is91ImmigrationRecord, bailRecord)
    Assertions.assertThat(is91ImmigrationRecord.homeOfficeReferenceNumber).isNull()
  }

  @Test
  fun `existing deportation order to IS91 keeps home office reference number`() {
    val homeOfficeReferenceNumber = "123456789"
    val is91ImmigrationRecord = immigrationDetentionEntity(homeOfficeReferenceNumber = null)
    val deportationOrder = immigrationDetentionEntity(immigrationDetentionRecordType = ImmigrationDetentionRecordType.DEPORTATION_ORDER, homeOfficeReferenceNumber = homeOfficeReferenceNumber)
    ImmigrationDetentionEntityUpdater.update(deportationOrder, is91ImmigrationRecord)
    Assertions.assertThat(deportationOrder.homeOfficeReferenceNumber).isEqualTo(homeOfficeReferenceNumber)
  }

  @Test
  fun `existing deportation order to bail clears the home office reference number`() {
    val homeOfficeReferenceNumber = "123456789"
    val deportationOrder = immigrationDetentionEntity(immigrationDetentionRecordType = ImmigrationDetentionRecordType.DEPORTATION_ORDER, homeOfficeReferenceNumber = homeOfficeReferenceNumber)
    val bailRecord = immigrationDetentionEntity(immigrationDetentionRecordType = ImmigrationDetentionRecordType.IMMIGRATION_BAIL, homeOfficeReferenceNumber = null)
    ImmigrationDetentionEntityUpdater.update(deportationOrder, bailRecord)
    Assertions.assertThat(deportationOrder.homeOfficeReferenceNumber).isNull()
  }

  @Test
  fun `bail to any other record type clears the home office reference number`() {
    val homeOfficeReferenceNumber = "123456789"
    val bailRecord = immigrationDetentionEntity(immigrationDetentionRecordType = ImmigrationDetentionRecordType.IMMIGRATION_BAIL, homeOfficeReferenceNumber = homeOfficeReferenceNumber)
    val is91ImmigrationRecord = immigrationDetentionEntity(homeOfficeReferenceNumber = null)
    ImmigrationDetentionEntityUpdater.update(bailRecord, is91ImmigrationRecord)
    Assertions.assertThat(bailRecord.homeOfficeReferenceNumber).isNull()
  }

  @Test
  fun `no longer of interest record to any other type clears the no longer of interest fields`() {
    val noLongerOfInterestRecord = immigrationDetentionEntity(immigrationDetentionRecordType = ImmigrationDetentionRecordType.NO_LONGER_OF_INTEREST, homeOfficeReferenceNumber = null, noLongerOfInterestReason = ImmigrationDetentionNoLongerOfInterestType.RIGHT_TO_REMAIN, noLongerOfInterestComment = "comment")
    val is91ImmigrationRecord = immigrationDetentionEntity(homeOfficeReferenceNumber = null)
    ImmigrationDetentionEntityUpdater.update(noLongerOfInterestRecord, is91ImmigrationRecord)
    Assertions.assertThat(noLongerOfInterestRecord.noLongerOfInterestReason).isNull()
    Assertions.assertThat(noLongerOfInterestRecord.noLongerOfInterestComment).isNull()
  }

  fun immigrationDetentionEntity(
    immigrationDetentionRecordType: ImmigrationDetentionRecordType = ImmigrationDetentionRecordType.IS91,
    prisonerId: String = "A123",
    recordDate: LocalDate = LocalDate.of(2021, 1, 1),
    homeOfficeReferenceNumber: String? = "123456789",
    noLongerOfInterestReason: ImmigrationDetentionNoLongerOfInterestType? = null,
    noLongerOfInterestComment: String? = null,
    statusId: ImmigrationDetentionEntityStatus = ImmigrationDetentionEntityStatus.ACTIVE,
    createdByUsername: String = "user",
  ): ImmigrationDetentionEntity = ImmigrationDetentionEntity(
    immigrationDetentionRecordType = immigrationDetentionRecordType,
    prisonerId = prisonerId,
    recordDate = recordDate,
    homeOfficeReferenceNumber = homeOfficeReferenceNumber,
    noLongerOfInterestReason = noLongerOfInterestReason,
    noLongerOfInterestComment = noLongerOfInterestComment,
    statusId = statusId,
    createdByUsername = createdByUsername,
  )
}
