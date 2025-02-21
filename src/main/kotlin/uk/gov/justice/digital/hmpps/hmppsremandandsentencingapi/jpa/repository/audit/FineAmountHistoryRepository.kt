package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit

import org.springframework.data.repository.CrudRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.FineAmountHistoryEntity

interface FineAmountHistoryRepository : CrudRepository<FineAmountHistoryEntity, Int>
