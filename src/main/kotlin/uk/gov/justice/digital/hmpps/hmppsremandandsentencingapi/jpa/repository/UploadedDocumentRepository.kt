package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository

import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.UploadedDocumentEntity
import java.time.ZonedDateTime
import java.util.UUID

interface UploadedDocumentRepository : CrudRepository<UploadedDocumentEntity, Int> {
  @Query("SELECT u FROM UploadedDocumentEntity u WHERE u.appearance.appearanceUuid = :appearanceUUID")
  fun findAllByAppearanceUUID(
    @Param("appearanceUUID") appearanceUUID: UUID,
  ): List<UploadedDocumentEntity>

  @Modifying
  @Query("delete from UploadedDocumentEntity d where d.appearance is null and d.createdAt < :cutoff")
  fun deleteWhenAppearanceIdIsNullAndCreatedAtOlderThan10Days(@Param("cutoff") cutoff: ZonedDateTime): Int

  fun findByDocumentUuid(documentUuid: UUID): UploadedDocumentEntity?
}
