package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto

data class AgencyDetails(val agencyId: String, val description: String, val agencyType: String, val active: Boolean, val addresses: List<Address>, val phones: List<Telephone>) {

  data class Address(val addressLine1: String?, val addressLine2: String?, val town: String?, val county: String?, val postcode: String?)
  data class Telephone(val number: String)
}
