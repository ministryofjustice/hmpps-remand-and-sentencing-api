package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.util

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import java.time.LocalDate

class SentenceUtils {
  companion object {
    fun calculateSentenceStartDate(sentenceEntity: SentenceEntity): LocalDate {
      val firstSentenceAppearance = sentenceEntity.charge.appearanceCharges
        .map { it.appearance!! }
        .filter { it.statusId == EntityStatus.ACTIVE && it.warrantType == "SENTENCING" }
        .minOfOrNull { it.appearanceDate }

      return firstSentenceAppearance ?: sentenceEntity.charge.appearanceCharges.map { it.appearance!! }.filter { it.statusId == EntityStatus.ACTIVE }.minOf { it.appearanceDate }
    }
  }
}
