package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto

import java.time.LocalDate

data class PrisonSearchDetails(val prisonId: String, val prisonName: String, val dateOfBirth: LocalDate)
