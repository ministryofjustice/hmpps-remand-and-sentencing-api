package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.util

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.NextCourtAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.service.LegacyAppearanceTypeService
import java.time.LocalTime

class CourtAppearanceUtils {
  companion object {
    fun getNOMISAppearanceTypeCode(courtAppearance: CourtAppearanceEntity, associatedNextCourtAppearance: NextCourtAppearanceEntity?): String = courtAppearance.legacyData?.nomisAppearanceTypeCode ?: associatedNextCourtAppearance?.courtAppearanceSubtype?.nomisCode ?: associatedNextCourtAppearance?.appearanceType?.dpsToNomisMappingCode ?: LegacyAppearanceTypeService.DEFAULT_APPEARANCE_TYPE_NOMIS_CODE

    fun getStartTime(courtAppearance: CourtAppearanceEntity, associatedNextCourtAppearance: NextCourtAppearanceEntity?): LocalTime = courtAppearance.legacyData?.appearanceTime ?: associatedNextCourtAppearance?.appearanceTime ?: LocalTime.MIDNIGHT
  }
}
