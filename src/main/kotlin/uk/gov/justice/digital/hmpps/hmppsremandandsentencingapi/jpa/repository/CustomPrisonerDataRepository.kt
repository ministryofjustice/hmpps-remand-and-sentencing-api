package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository

interface CustomPrisonerDataRepository {
  fun deletePrisonerData(prisonerId: String)
}
