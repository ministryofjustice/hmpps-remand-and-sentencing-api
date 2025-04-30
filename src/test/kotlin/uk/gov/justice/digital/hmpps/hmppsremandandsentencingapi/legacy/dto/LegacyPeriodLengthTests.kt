package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.dto

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.PeriodLengthEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceTypeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.PeriodLengthType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ReferenceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceTypeClassification
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyPeriodLength
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
    assertThat(result.sentenceUuid).isEqualTo(testSentenceUuid)
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
    )

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
      chargeNumber = "TEST123",
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
