package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.util

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.AppearanceChargeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtCaseEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator

class SentenceUtilsTests {

  @Test
  fun `calculate sentence start date to first sentenced appearance`() {
    val charge = getSentencedCharge()
    val sentence = SentenceEntity.from(DpsDataCreator.dpsCreateSentence(), username, charge, null, null)
    val sentenceStartDate = SentenceUtils.calculateSentenceStartDate(sentence)
    Assertions.assertEquals(sentencedCourtAppearance.appearanceDate, sentenceStartDate)
  }

  @Test
  fun `fallback to the first court appearance when no sentenced court appearance`() {
    val remandCourtAppearance = CourtAppearanceEntity.from(DpsDataCreator.dpsCreateCourtAppearance(warrantType = "REMAND"), null, courtCase, username)
    val charge = ChargeEntity.from(DpsDataCreator.dpsCreateCharge(), null, username)
    charge.appearanceCharges.add(AppearanceChargeEntity(sentencedCourtAppearance, Companion.charge, username, null))
    val sentence = SentenceEntity.from(DpsDataCreator.dpsCreateSentence(), username, charge, null, null)
    val sentenceStartDate = SentenceUtils.calculateSentenceStartDate(sentence)
    Assertions.assertEquals(remandCourtAppearance.appearanceDate, sentenceStartDate)
  }

  @Test
  fun `process recall appearances with sentences on them`() {
    val recalledAppearance = CourtAppearanceEntity.from(DpsDataCreator.dpsCreateCourtAppearance(), null, courtCase, username)
    recalledAppearance.statusId = EntityStatus.RECALL_APPEARANCE
    val charge = ChargeEntity.from(DpsDataCreator.dpsCreateCharge(), null, username)
    charge.appearanceCharges.add(
      AppearanceChargeEntity(
        recalledAppearance,
        Companion.charge,
        username,
        null,
      ),
    )
    val sentence = SentenceEntity.from(DpsDataCreator.dpsCreateSentence(), username, charge, null, null)
    val sentenceStartDate = SentenceUtils.calculateSentenceStartDate(sentence)
    Assertions.assertEquals(recalledAppearance.appearanceDate, sentenceStartDate)
  }

  companion object {
    val username = "user"
    val courtCase = CourtCaseEntity.from(DpsDataCreator.dpsCreateCourtCase(), username)
    val sentencedCourtAppearance = CourtAppearanceEntity.from(DpsDataCreator.dpsCreateCourtAppearance(), null, courtCase, username)
    val charge = ChargeEntity.from(DpsDataCreator.dpsCreateCharge(), null, username)

    fun getSentencedCharge(): ChargeEntity {
      charge.appearanceCharges.add(AppearanceChargeEntity(sentencedCourtAppearance, charge, username, null))
      return charge
    }
  }
}
