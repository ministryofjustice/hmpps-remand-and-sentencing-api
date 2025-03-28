package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository

import org.springframework.data.repository.CrudRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.LegacySentenceTypeEntity

interface LegacySentenceTypeRepository : CrudRepository<LegacySentenceTypeEntity, Int> {

  fun findByNomisSentenceTypeReference(legacySentenceTypeReference: String): List<LegacySentenceTypeEntity>
}
