package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateFineAmount
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceTypeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ReferenceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceTypeClassification
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import java.math.BigDecimal
import java.util.*

class SentenceEntityTests {

  @Test
  fun `can handle comparing null fine amount to actual fine amount`() {
    val chargeEntity = ChargeEntity.from(DpsDataCreator.dpsCreateCharge(), null, "USER")
    val sentenceType = SentenceTypeEntity(sentenceTypeUuid = UUID.randomUUID(), description = "fine", minAgeInclusive = null, maxAgeExclusive = null, minDateInclusive = null, maxDateExclusive = null, minOffenceDateInclusive = null, maxOffenceDateExclusive = null, classification = SentenceTypeClassification.FINE, hintText = null, nomisCjaCode = "", nomisSentenceCalcType = "", displayOrder = 1, status = ReferenceEntityStatus.ACTIVE, isRecallable = true)
    val sentenceEntity = SentenceEntity.from(DpsDataCreator.dpsCreateSentence(fineAmount = null), "USER", chargeEntity, null, sentenceType)
    val compareSentenceEntity = SentenceEntity.from(
      DpsDataCreator.dpsCreateSentence(
        fineAmount = CreateFineAmount(
          BigDecimal.TEN,
        ),
      ),
      "USER",
      chargeEntity,
      null,
      sentenceType,
    )
    val result = sentenceEntity.isSame(compareSentenceEntity)
    Assertions.assertThat(result).isFalse
  }
}
