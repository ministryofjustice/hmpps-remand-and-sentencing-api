package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.listener.dto

data class PrisonerBookingMovedEvent(
  val additionalInformation: PrisonerBookingMovedAdditionalInformation,
  val eventType: String,
  val occurredAt: String,
  val personReference: PersonReference,
  val publishedAt: String,
  val version: Int,
)

data class PrisonerBookingMovedAdditionalInformation(
  val bookingId: String,
  val movedFromNomsNumber: String,
  val movedToNomsNumber: String,
  val bookingStartDateTime: String,
)

data class PersonReference(
  val identifiers: List<Identifier>,
)

data class Identifier(
  val type: String,
  val value: String,
)
