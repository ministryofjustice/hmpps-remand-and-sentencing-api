package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.projection

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventMetadata
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.RecordEventMetadata
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtCaseEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceEntity

data class CourtCaseAppearanceChargeSentence(
  val courtCase: CourtCaseEntity,
  val appearance: CourtAppearanceEntity,
  val charge: ChargeEntity,
  val sentence: SentenceEntity,
) {
  fun <T> toRecordEventMetadata(record: T): RecordEventMetadata<T> = RecordEventMetadata(
    record,
    EventMetadata(
      courtCase.prisonerId,
      courtCase.caseUniqueIdentifier,
      appearance.appearanceUuid.toString(),
      charge.chargeUuid.toString(),
      sentence.sentenceUuid.toString(),
      null,
      EventType.METADATA_ONLY,
    ),
  )
}
