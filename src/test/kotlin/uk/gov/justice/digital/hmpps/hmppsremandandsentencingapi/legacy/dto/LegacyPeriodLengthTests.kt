package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.dto

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.AppearanceChargeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtCaseEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.PeriodLengthEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceTypeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.PeriodLengthType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ReferenceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceTypeClassification
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyPeriodLength
import java.time.LocalDate
import java.util.UUID

class LegacyPeriodLengthTests {
  @Test
  fun `return no units when its a life sentence`() {
    val result = LegacyPeriodLength.from(testPeriodLength, testSentence)

    assertThat(result.periodYears).isNull()
    assertThat(result.periodMonths).isNull()
    assertThat(result.periodWeeks).isNull()
    assertThat(result.periodDays).isNull()
    assertThat(result.isLifeSentence).isTrue()
    assertThat(result.sentenceUuid).isEqualTo(testSentence.sentenceUuid)
    assertThat(result.prisonerId).isEqualTo(TEST_PRISONER_ID)
  }

  @Test
  fun `legacy sentence with DPS period length`() {
    val legacySentence = SentenceEntity(
      sentenceUuid = testSentenceUuid,
      statusId = EntityStatus.ACTIVE,
      createdBy = "USER",
      sentenceServeType = "CONCURRENT",
      consecutiveTo = null,
      supersedingSentence = null,
      charge = testCharge,
      convictionDate = null,
      legacyData = DataCreator.sentenceLegacyData(),
      fineAmount = null,
      countNumber = null,
      sentenceType = null,
    )
    val result = LegacyPeriodLength.from(testPeriodLength, legacySentence)
  }

  companion object {
    private val testPeriodLength = PeriodLengthEntity(
      periodLengthUuid = UUID.randomUUID(),
      years = 15,
      months = 5,
      weeks = 2,
      days = 1,
      createdPrison = null,
      sentenceEntity = null,
      appearanceEntity = null,
      statusId = EntityStatus.ACTIVE,
      periodOrder = "years,months,weeks,days",
      periodLengthType = PeriodLengthType.TARIFF_LENGTH,
      createdBy = "createdBy",
    )

    private const val TEST_PRISONER_ID = "A1234BC"

    private val testCourtCase = CourtCaseEntity(
      caseUniqueIdentifier = "CASE123",
      prisonerId = TEST_PRISONER_ID,
      statusId = EntityStatus.ACTIVE,
      createdBy = "test-user",
    )

    private val testCourtAppearance = CourtAppearanceEntity(
      id = 0,
      appearanceUuid = UUID.randomUUID(),
      courtCase = testCourtCase,
      courtCode = "COURT1",
      appearanceDate = LocalDate.now(),
      statusId = EntityStatus.ACTIVE,
      createdBy = "test-user",
      createdPrison = "TEST",
      warrantType = "TEST",
      appearanceOutcome = null,
      courtCaseReference = null,
      previousAppearance = null,
      warrantId = null,
      updatedAt = null,
      updatedBy = null,
      updatedPrison = null,
      appearanceCharges = mutableSetOf(),
      nextCourtAppearance = null,
      overallConvictionDate = null,
      legacyData = null,
    )

    private val testCharge = ChargeEntity(
      chargeUuid = UUID.randomUUID(),
      offenceCode = "TEST123",
      statusId = EntityStatus.ACTIVE,
      createdBy = "test-user",
      offenceStartDate = null,
      offenceEndDate = null,
      chargeOutcome = null,
      supersedingCharge = null,
      terrorRelated = null,
      createdPrison = null,
      legacyData = null,
      appearanceCharges = mutableSetOf(),
    ).apply {
      appearanceCharges.add(
        AppearanceChargeEntity(
          courtAppearanceEntity = testCourtAppearance,
          chargeEntity = this,
          createdBy = "test-user",
          createdPrison = "TEST",
        ),
      )
    }

    private val testSentenceType = SentenceTypeEntity(
      sentenceTypeUuid = UUID.randomUUID(),
      description = "Life sentence",
      classification = SentenceTypeClassification.INDETERMINATE,
      nomisCjaCode = "LIFE",
      nomisSentenceCalcType = "LIFE",
      displayOrder = 1,
      status = ReferenceEntityStatus.ACTIVE,
      minAgeInclusive = null,
      maxAgeExclusive = null,
      minDateInclusive = null,
      maxDateExclusive = null,
      minOffenceDateInclusive = null,
      maxOffenceDateExclusive = null,
      hintText = null,
    )

    private val testSentenceUuid = UUID.randomUUID()

    private val testSentence = SentenceEntity(
      sentenceUuid = testSentenceUuid,
      countNumber = "TEST123",
      statusId = EntityStatus.ACTIVE,
      createdBy = "test-user",
      createdPrison = "TEST",
      sentenceServeType = "LIFE",
      consecutiveTo = null,
      sentenceType = testSentenceType,
      supersedingSentence = null,
      charge = testCharge,
      convictionDate = null,
      fineAmount = null,
    )
  }
}
