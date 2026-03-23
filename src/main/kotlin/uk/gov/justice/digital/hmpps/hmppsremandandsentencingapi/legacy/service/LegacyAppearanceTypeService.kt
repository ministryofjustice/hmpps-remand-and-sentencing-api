package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.AppearanceTypeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.AppearanceTypeRepository
import java.util.*

@Service
class LegacyAppearanceTypeService(private val appearanceTypeRepository: AppearanceTypeRepository) {

  fun getAppearanceType(nomisAppearanceTypeCode: String?, appearanceTypeUuid: UUID?): AppearanceTypeEntity = if (nomisAppearanceTypeCode != null) {
    appearanceTypeRepository.findByNomisCode(nomisAppearanceTypeCode) ?: getDefaultAppearanceType()
  } else if (appearanceTypeUuid != null) {
    appearanceTypeRepository.findByAppearanceTypeUuid(appearanceTypeUuid) ?: getDefaultAppearanceType()
  } else {
    getDefaultAppearanceType()
  }

  fun getDefaultAppearanceType(): AppearanceTypeEntity = appearanceTypeRepository.findByAppearanceTypeUuid(DEFAULT_APPEARANCE_TYPE_UUID)!!

  companion object {
    val DEFAULT_APPEARANCE_TYPE_UUID: UUID = UUID.fromString("63e8fce0-033c-46ad-9edf-391b802d547a") // Court appearance
    const val DEFAULT_APPEARANCE_TYPE_NOMIS_CODE: String = "CRT"
  }
}
