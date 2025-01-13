package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.error

class ChargeAlreadySentencedException(override var message: String) : RuntimeException(message)
