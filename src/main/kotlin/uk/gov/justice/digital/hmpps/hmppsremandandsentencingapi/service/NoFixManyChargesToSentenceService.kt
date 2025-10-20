package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventMetadata
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.RecordEventMetadata
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtCaseEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceEntity
import java.util.UUID

@ConditionalOnProperty(
  name = ["features.fix.many.charges.to.sentence"],
  havingValue = "disabled",
)
@Service
class NoFixManyChargesToSentenceService : FixManyChargesToSentenceService {
  override fun fixCourtCaseSentences(courtCases: List<CourtCaseEntity>): MutableSet<EventMetadata> = mutableSetOf()

  override fun fixCourtCasesById(courtCaseIds: Set<Int>): MutableSet<EventMetadata> = mutableSetOf()

  override fun fixSentencesBySentenceUuids(sentenceUuids: List<RecordEventMetadata<UUID>>): MutableSet<EventMetadata> = mutableSetOf()

  override fun fixSentences(sentences: List<RecordEventMetadata<SentenceEntity>>): MutableSet<EventMetadata> = mutableSetOf()
}
