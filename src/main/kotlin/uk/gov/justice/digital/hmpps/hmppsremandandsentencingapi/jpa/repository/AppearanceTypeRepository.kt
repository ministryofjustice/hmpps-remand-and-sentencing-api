package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.AppearanceTypeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ReferenceEntityStatus
import java.util.UUID

interface AppearanceTypeRepository : CrudRepository<AppearanceTypeEntity, Int> {
  fun findByAppearanceTypeUuid(appearanceTypeUuid: UUID): AppearanceTypeEntity?

  fun findByStatusIn(statuses: List<ReferenceEntityStatus>): List<AppearanceTypeEntity>

  @Query(
    """
    select at.*, (select count(*) from court_appearance_subtype cas where cas.appearance_type_id= at.id) as totalSubtypes
    from appearance_type at, jsonb_array_elements(at.nomis_to_dps_mapping_codes->'codes') nomisCodesObj
    where nomisCodesObj->>'code' = :nomisCode
  """,
    nativeQuery = true,
  )
  fun findByNomisCode(@Param("nomisCode")nomisCode: String): AppearanceTypeEntity?
}
