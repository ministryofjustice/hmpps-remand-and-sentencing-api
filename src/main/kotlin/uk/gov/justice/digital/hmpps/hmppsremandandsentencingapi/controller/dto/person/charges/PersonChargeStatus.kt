package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.person.charges

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.CourtCaseEntityStatus

enum class PersonChargeStatus(val courtCaseStatuses: List<CourtCaseEntityStatus>) {
  ACTIVE(listOf(CourtCaseEntityStatus.ACTIVE)),
  INACTIVE(listOf(CourtCaseEntityStatus.INACTIVE, CourtCaseEntityStatus.MERGED)),
}
