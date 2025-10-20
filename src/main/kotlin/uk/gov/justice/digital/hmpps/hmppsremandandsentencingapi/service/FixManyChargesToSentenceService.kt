package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventMetadata
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.RecordEventMetadata
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtCaseEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceEntity
import java.util.UUID

interface FixManyChargesToSentenceService {

  fun fixCourtCaseSentences(courtCases: List<CourtCaseEntity>): MutableSet<EventMetadata>

  fun fixCourtCasesById(courtCaseIds: Set<Int>): MutableSet<EventMetadata>

  fun fixSentencesBySentenceUuids(sentenceUuids: List<RecordEventMetadata<UUID>>): MutableSet<EventMetadata>

  fun fixSentences(sentences: List<RecordEventMetadata<SentenceEntity>>): MutableSet<EventMetadata>
}
