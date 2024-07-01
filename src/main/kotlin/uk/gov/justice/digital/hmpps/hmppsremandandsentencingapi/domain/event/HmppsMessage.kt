package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event

data class HmppsMessage<T>(val eventType: String, val version: Int, val description: String, val detailUrl: String, val occurredAt: String, val additionalInformation: T, val personReference: PersonReference)

data class PersonReference(val identifiers: List<PersonReferenceType>)

data class PersonReferenceType(val type: String, val value: String)
