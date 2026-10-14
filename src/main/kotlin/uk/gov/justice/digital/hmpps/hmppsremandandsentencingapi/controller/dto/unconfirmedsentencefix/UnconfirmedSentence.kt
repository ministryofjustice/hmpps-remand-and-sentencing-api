package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.unconfirmedsentencefix

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.projection.UnconfirmedSentenceFixCourtCaseRow
import java.util.UUID

data class UnconfirmedSentence(
  val sentenceUuid: UUID,
  val offenceCode: String,
  val chargeLegacyOffenceDescription: String?,
  val sentenceServeType: String,
) {
  companion object {
    fun from(row: UnconfirmedSentenceFixCourtCaseRow): UnconfirmedSentence = UnconfirmedSentence(row.sentenceUuid, row.offenceCode, row.chargeLegacyOffenceDescription, row.sentenceServeType)
  }
}
