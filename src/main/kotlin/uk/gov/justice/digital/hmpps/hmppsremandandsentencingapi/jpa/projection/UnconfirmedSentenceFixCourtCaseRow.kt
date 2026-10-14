package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.projection

import java.time.LocalDate
import java.util.UUID

data class UnconfirmedSentenceFixCourtCaseRow(
  val courtCaseUuid: String,
  val caseReference: String?,
  val appearanceDate: LocalDate,
  val courtCode: String,
  val chargeLegacyOffenceDescription: String?,
  val offenceCode: String,
  val sentenceUuid: UUID,
  val sentenceServeType: String,
)
