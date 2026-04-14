package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceTypeEntity

data class UpdatedSentenceType(
  val entity: SentenceTypeEntity,
  val migrateSentenceData: Boolean,
)
