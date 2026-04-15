package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.sentencetypes

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceTypeEntity

data class AllSentenceTypes(
  val sentenceTypes: List<SentenceTypeDetails>,
) {
  companion object {
    fun from(sentenceTypeEntities: List<SentenceTypeEntity>): AllSentenceTypes = AllSentenceTypes(sentenceTypeEntities.map { SentenceTypeDetails.from(it) })
  }
}
