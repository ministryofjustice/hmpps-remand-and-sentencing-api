package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.PeriodLengthEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceTypeEntity
import java.util.UUID

data class CourtCaseHierarchyData(
  var prisonerId: String,
  var courtCaseId: String?,
  var courtAppearanceUuid: UUID?,
  var courtAppearanceDateChanged: Boolean? = false,
  var existingSentenceType: SentenceTypeEntity? = null,
  var updatedSentenceType: SentenceTypeEntity? = null,
  val courtAppearancePeriodLengths: MutableSet<PeriodLengthEntity> = mutableSetOf(),
  val isBreach: Boolean = false,
)
