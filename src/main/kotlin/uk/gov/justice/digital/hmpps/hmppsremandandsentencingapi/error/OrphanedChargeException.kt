package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.error

class OrphanedChargeException(override var message: String) : RuntimeException(message)
