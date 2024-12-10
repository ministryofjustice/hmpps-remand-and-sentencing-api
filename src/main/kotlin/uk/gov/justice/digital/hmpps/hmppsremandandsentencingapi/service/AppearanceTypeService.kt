package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.AppearanceType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.AppearanceTypeRepository
import java.util.UUID

@Service
class AppearanceTypeService(private val appearanceTypeRepository: AppearanceTypeRepository) {

  fun getAll(): List<AppearanceType> = appearanceTypeRepository.findAll().map { AppearanceType.from(it) }

  fun findByUuid(appearanceTypeUuid: UUID): AppearanceType? = appearanceTypeRepository.findByAppearanceTypeUuid(appearanceTypeUuid)?.let { AppearanceType.from(it) }
}
