package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository

import org.springframework.data.repository.CrudRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ImmigrationDetentionEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ImmigrationDetentionEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ImmigrationDetentionEntityStatus.ACTIVE
import java.util.UUID

interface ImmigrationDetentionRepository : CrudRepository<ImmigrationDetentionEntity, Int> {
  fun findOneByImmigrationDetentionUuid(immigrationDetentionUUID: UUID): ImmigrationDetentionEntity?
  fun findByPrisonerIdAndStatusId(
    prisonerId: String,
    statusId: ImmigrationDetentionEntityStatus = ACTIVE,
  ): List<ImmigrationDetentionEntity>

  fun findTop1ByPrisonerIdAndStatusIdOrderByRecordDateDescCreatedAtDesc(
    prisonerId: String,
    statusId: ImmigrationDetentionEntityStatus = ACTIVE,
  ): ImmigrationDetentionEntity?

  fun findByCourtAppearanceUuidAndStatusId(courtAppearanceUuid: UUID, statusId: ImmigrationDetentionEntityStatus = ACTIVE): List<ImmigrationDetentionEntity>
}
