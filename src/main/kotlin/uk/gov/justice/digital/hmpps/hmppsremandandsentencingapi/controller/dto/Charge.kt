package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.ChargeLegacyData
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

data class Charge(
  val chargeUuid: UUID,
  val offenceCode: String,
  val offenceStartDate: LocalDate?,
  val offenceEndDate: LocalDate?,
  val outcome: ChargeOutcome?,
  val terrorRelated: Boolean?,
  val foreignPowerRelated: Boolean?,
  val sentence: Sentence?,
  val legacyData: ChargeLegacyData?,
  val mergedFromCase: MergedFromCase?,
  val createdAt: ZonedDateTime,
) {
  companion object {
    fun from(chargeEntity: ChargeEntity, getSentenceFunction: java.util.function.Function<ChargeEntity, SentenceEntity?> = { it.getLiveSentence() }): Charge = Charge(
      chargeEntity.chargeUuid,
      chargeEntity.offenceCode,
      chargeEntity.offenceStartDate,
      chargeEntity.offenceEndDate,
      chargeEntity.chargeOutcome?.let { ChargeOutcome.from(it) },
      chargeEntity.terrorRelated,
      chargeEntity.foreignPowerRelated,
      getSentenceFunction.apply(chargeEntity)?.let { Sentence.from(it) },
      chargeEntity.legacyData,
      chargeEntity.mergedFromCourtCase?.latestCourtAppearance?.let {
        MergedFromCase.from(
          it,
          chargeEntity.mergedFromDate,
        )
      },
      chargeEntity.createdAt,
    )
  }
}
