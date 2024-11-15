package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.legacy.controller.dto.LegacyCreateCourtCase

class DataCreator {
  companion object Factory {
    fun legacyCreateCourtCase(prisonerId: String = "PRI123", active: Boolean = true): LegacyCreateCourtCase {
      return LegacyCreateCourtCase(prisonerId, active)
    }
  }
}
