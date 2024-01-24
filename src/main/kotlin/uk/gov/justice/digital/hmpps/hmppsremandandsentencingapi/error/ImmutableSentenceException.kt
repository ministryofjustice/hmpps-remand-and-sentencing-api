package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.error

class ImmutableSentenceException(override var message: String) : RuntimeException(message)
