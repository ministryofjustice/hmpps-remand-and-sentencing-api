package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.dto

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.PeriodLengthEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.PeriodLengthType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceTypeClassification
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyPeriodLength
import java.util.UUID

class LegacyPeriodLengthTests {

  @Test
  fun `return no units when its a life sentence`() {
    val periodLengthEntity = PeriodLengthEntity(
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
    val sentenceUuid = UUID.randomUUID()
    val result = LegacyPeriodLength.from(periodLengthEntity, SentenceTypeClassification.INDETERMINATE, sentenceUuid)
    Assertions.assertThat(result.periodYears).isNull()
    Assertions.assertThat(result.periodMonths).isNull()
    Assertions.assertThat(result.periodWeeks).isNull()
    Assertions.assertThat(result.periodDays).isNull()
    Assertions.assertThat(result.isLifeSentence).isTrue
    Assertions.assertThat(result.sentenceUuid).isEqualTo(sentenceUuid)
  }
}
