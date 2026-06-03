package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.batch

import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.batch.ManyChargesToSentenceFixQueueEntity

interface MultipleChargesSingleSentenceFixQueueRepository : CrudRepository<ManyChargesToSentenceFixQueueEntity, Long> {

  @Modifying
  @Query(value = "TRUNCATE TABLE many_charges_to_sentence_fix_queue", nativeQuery = true)
  fun truncate()

  @Modifying
  @Query(
    value = """INSERT INTO many_charges_to_sentence_fix_queue (case_unique_identifier)
               SELECT cc.case_unique_identifier      
               FROM court_case cc      
               WHERE EXISTS (SELECT 1 
                             FROM court_appearance lcap 
                             JOIN appearance_charge ac ON ac.appearance_id = lcap.id
                             JOIN charge c ON c.id = ac.charge_id
                             JOIN sentence s ON s.charge_id = c.id
                             WHERE lcap.id = cc.latest_court_appearance_id 
                             AND s.status_id = 'MANY_CHARGES_DATA_FIX')      
               ORDER BY cc.updated_at DESC      
                LIMIT :limit    
           """,
    nativeQuery = true,
  )
  fun populateQueue(@Param("limit") limit: Int)
}
