package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.custom

interface CustomPrisonerDataRepository {
  fun deletePrisonerData(prisonerId: String)
}