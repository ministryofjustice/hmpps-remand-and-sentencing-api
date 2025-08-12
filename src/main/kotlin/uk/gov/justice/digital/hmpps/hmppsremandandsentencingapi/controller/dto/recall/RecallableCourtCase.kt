package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.recall

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.CourtAppearanceLegacyData
import java.time.LocalDate

data class RecallableCourtCase(
  val courtCaseUuid: String,
  val reference: String,
  val courtCode: String,
  val status: EntityStatus,
  val isSentenced: Boolean,
  val sentences: List<RecallableCourtCaseSentence>,
  val warrantDate: LocalDate,
  val warrantType: String,
  val outcome: String?,
  val caseReferences: List<String>,
  val firstDayInCustody: LocalDate?,
  val legacyCourtAppearance: CourtAppearanceLegacyData?,
)
