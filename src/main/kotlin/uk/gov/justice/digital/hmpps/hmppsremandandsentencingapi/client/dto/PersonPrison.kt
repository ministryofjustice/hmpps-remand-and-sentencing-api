package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto

data class PersonPrison(val firstName: String?, val lastName: String?, val sex: Gender?) {

  data class Gender(val code: String?, val description: String?)
}
