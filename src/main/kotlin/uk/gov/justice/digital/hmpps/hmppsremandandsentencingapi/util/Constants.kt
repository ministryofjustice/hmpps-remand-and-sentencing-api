package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util

import java.util.UUID

class Constants {
  companion object {
    val breachWarrantTypes = setOf("BREACH_OF_SUPERVISION_REQUIREMENTS", "BREACH_OF_IMPRISONABLE_OFFENCE")
    val nilUUID = UUID(0L, 0L)
    const val ZONE_ID = "Europe/London"
    const val NEXT_COURT_APPEARANCE_PERSON_CACHE_KEY = "nextCourtAppearancePerson"
    const val PRISONER_SEARCH_API_CACHE_KEY = "prisonerSearchApi"
    const val COURT_REGISTER_BY_COURT_CODE_CACHE_KEY = "courtRegisterByCourtCode"
    const val PERSON_PRISON_CACHE_KEY = "personPrisonCached"
    const val MANAGE_OFFENCE_DETAILS_CACHE_KEY = "manageOffencesDetails"
    const val PRISON_REGISTER_AGENCY_DETAILS_CACHE_KEY = "prisonRegisterAgencyDetails"
  }
}
