package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.courtappearanceschedule.UpdateCourtAppearanceSchedule
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.AppearanceTypeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtAppearanceSubtypeEntity
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class CourtAppearanceLegacyData(
  val postedDate: String?,
  val nomisOutcomeCode: String?,
  val outcomeDescription: String?,
  val nextEventDateTime: LocalDateTime?,
  val appearanceTime: LocalTime?,
  val outcomeDispositionCode: String?,
  val outcomeConvictionFlag: Boolean?,
  val nomisAppearanceTypeCode: String?,
  val comments: String?,
) {
  fun isSame(other: CourtAppearanceLegacyData?): Boolean = nomisOutcomeCode == other?.nomisOutcomeCode &&
    outcomeDescription == other?.outcomeDescription &&
    nextEventDateTime == other?.nextEventDateTime &&
    appearanceTime == other?.appearanceTime &&
    outcomeDispositionCode == other?.outcomeDispositionCode &&
    outcomeConvictionFlag == other?.outcomeConvictionFlag &&
    nomisAppearanceTypeCode == other?.nomisAppearanceTypeCode &&
    comments == other?.comments

  fun copyFrom(appearanceTime: LocalTime?, appearanceType: AppearanceTypeEntity, courtAppearanceSubtypeEntity: CourtAppearanceSubtypeEntity?): CourtAppearanceLegacyData = CourtAppearanceLegacyData(
    LocalDate.now().format(DateTimeFormatter.ISO_DATE),
    nomisOutcomeCode,
    outcomeDescription,
    nextEventDateTime,
    appearanceTime,
    outcomeDispositionCode,
    outcomeConvictionFlag,
    courtAppearanceSubtypeEntity?.nomisCode ?: appearanceType.dpsToNomisMappingCode,
    comments,
  )
  companion object {
    fun from(appearanceTime: LocalTime, appearanceType: AppearanceTypeEntity, courtAppearanceSubtypeEntity: CourtAppearanceSubtypeEntity?): CourtAppearanceLegacyData = CourtAppearanceLegacyData(
      LocalDate.now().format(DateTimeFormatter.ISO_DATE),
      null,
      null,
      null,
      appearanceTime,
      null,
      null,
      courtAppearanceSubtypeEntity?.nomisCode ?: appearanceType.dpsToNomisMappingCode,
      null,
    )

    fun from(updateCourtAppearanceSchedule: UpdateCourtAppearanceSchedule): CourtAppearanceLegacyData = CourtAppearanceLegacyData(
      LocalDate.now().format(DateTimeFormatter.ISO_DATE),
      null,
      null,
      null,
      updateCourtAppearanceSchedule.start.toLocalTime(),
      null,
      null,
      updateCourtAppearanceSchedule.reasonCode,
      updateCourtAppearanceSchedule.comments,
    )
  }
}
