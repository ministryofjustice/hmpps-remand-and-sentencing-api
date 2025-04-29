package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository
import org.springframework.data.domain.Sort
import org.springframework.data.repository.CrudRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.LegacySentenceTypeEntity

interface LegacySentenceTypeRepository : CrudRepository<LegacySentenceTypeEntity, Int> {

  fun findByNomisSentenceTypeReference(
    legacySentenceTypeReference: String,
    sort: Sort,
  ): List<LegacySentenceTypeEntity>

  fun findAll(sort: Sort): List<LegacySentenceTypeEntity>

  fun findByNomisSentenceTypeReferenceInAndSentencingActIn(
    legacySentenceTypeReference: List<String>,
    sentencingAct: List<Int>,
  ): List<LegacySentenceTypeEntity>

  fun findByNomisSentenceTypeReferenceAndSentencingAct(
    legacySentenceTypeReference: String,
    sentencingAct: Int,
  ): LegacySentenceTypeEntity?
}
