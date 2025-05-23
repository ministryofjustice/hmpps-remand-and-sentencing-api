package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.UploadedDocumentEntity
import java.util.Optional
import java.util.UUID

interface UploadedDocumentRepository : CrudRepository<UploadedDocumentEntity, Int> {
  @Query("SELECT u FROM UploadedDocumentEntity u WHERE u.appearance.appearanceUuid = :appearanceUUID AND u.warrantType = :warrantType")
  fun findAllByAppearanceUUIDAndWarrantType(
    @Param("appearanceUUID") appearanceUUID: UUID,
    @Param("warrantType") warrantType: String,
  ): List<UploadedDocumentEntity>

  fun findByDocumentUuid(documentUuid: UUID): Optional<UploadedDocumentEntity>
}