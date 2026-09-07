package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util

import java.util.UUID

class Constants {
  companion object {
    val breachWarrantTypes = setOf("BREACH_OF_SUPERVISION_REQUIREMENTS", "BREACH_OF_IMPRISONABLE_OFFENCE")
    val nilUUID = UUID(0L, 0L)
  }
}
