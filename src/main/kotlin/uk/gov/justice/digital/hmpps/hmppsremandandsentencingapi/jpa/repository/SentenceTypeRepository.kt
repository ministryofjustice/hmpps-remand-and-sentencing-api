package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceTypeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceTypeClassification
import java.time.LocalDate
import java.util.UUID

interface SentenceTypeRepository : CrudRepository<SentenceTypeEntity, Int> {

  @Query(
    """
    select ste from SentenceTypeEntity ste where 
    (ste.minAgeInclusive <=:age or ste.minAgeInclusive is null) and 
    (ste.maxAgeExclusive > :age or ste.maxAgeExclusive is null) and
    (ste.minDateInclusive <= :convictionDate or ste.minDateInclusive is null) and
    (ste.maxDateExclusive > :convictionDate or ste.maxDateExclusive is null) and
    ste.classification !=:classification""",
  )
  fun findByAgeInAndConvictionDateInAndClassificationNot(age: Int, convictionDate: LocalDate, classification: SentenceTypeClassification): List<SentenceTypeEntity>

  fun findBySentenceTypeUuid(sentenceTypeUuid: UUID): SentenceTypeEntity?

  fun findBySentenceTypeUuidIn(sentenceTypeUuids: List<UUID>): List<SentenceTypeEntity>

  fun findByNomisCjaCodeAndNomisSentenceCalcType(nomisCjaCode: String, nomisSentenceCalcType: String): SentenceTypeEntity?

  fun findByNomisCjaCodeInAndNomisSentenceCalcTypeIn(nomisCjaCodes: List<String>, nomisSentenceCalcTypes: List<String>): List<SentenceTypeEntity>
}
