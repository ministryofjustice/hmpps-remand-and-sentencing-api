package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceTypeEntity
import java.time.LocalDate
import java.util.UUID

interface SentenceTypeRepository : CrudRepository<SentenceTypeEntity, Int> {

  @Query(
    """
    select ste from SentenceTypeEntity ste where 
    (ste.minAgeInclusive <=:age or ste.minAgeInclusive is null) and 
    (ste.maxAgeExclusive > :age or ste.maxAgeExclusive is null) and
    (ste.minDateInclusive <= :convictionDate or ste.minDateInclusive is null) and
    (ste.maxDateExclusive > :convictionDate or ste.maxDateExclusive is null)""",
  )
  fun findByAgeInAndConvictionDateIn(age: Int, convictionDate: LocalDate): List<SentenceTypeEntity>

  fun findBySentenceTypeUuid(sentenceTypeUuid: UUID): SentenceTypeEntity?
}
