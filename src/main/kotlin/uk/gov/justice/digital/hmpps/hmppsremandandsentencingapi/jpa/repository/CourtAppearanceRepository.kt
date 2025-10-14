package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtCaseEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import java.time.LocalDate
import java.util.*

interface CourtAppearanceRepository : CrudRepository<CourtAppearanceEntity, Int> {
  fun findByAppearanceUuid(appearanceUuid: UUID): CourtAppearanceEntity?

  @Query(
    value = """
    SELECT ca.* from
    court_appearance ca
    join court_case cc on cc.id=ca.court_case_id
    where cc.id= :courtCaseId and (ca.legacy_data->>'nextEventDateTime')::date = :nextEventDate
    order by ca.appearance_date desc
    limit 1
  """,
    nativeQuery = true,
  )
  fun findByNextEventDateTime(courtCaseId: Int, nextEventDate: LocalDate): CourtAppearanceEntity?

  fun findFirstByCourtCaseAndStatusIdOrderByAppearanceDateDesc(courtCaseEntity: CourtCaseEntity, status: EntityStatus): CourtAppearanceEntity?

  fun findByCourtCaseCaseUniqueIdentifierAndStatusId(courtCaseUuid: String, status: EntityStatus): CourtAppearanceEntity
}
