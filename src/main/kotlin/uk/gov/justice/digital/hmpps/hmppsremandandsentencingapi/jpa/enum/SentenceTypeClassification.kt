package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum

enum class SentenceTypeClassification {
  STANDARD,
  EXTENDED,
  SOPC,
  INDETERMINATE,
  BOTUS,
  CIVIL,
  DTO,
  FINE,
  LEGACY,
  NON_CUSTODIAL,
  LEGACY_RECALL,
  UNKNOWN,
  ;

  companion object {
    fun from(classification: String): SentenceTypeClassification = entries.firstOrNull { it.name == classification } ?: UNKNOWN
  }
}
