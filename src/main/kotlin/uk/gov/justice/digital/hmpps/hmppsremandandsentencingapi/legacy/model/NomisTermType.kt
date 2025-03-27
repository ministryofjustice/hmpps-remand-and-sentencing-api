package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.model

enum class NomisTermType(var description: String = "") {
  COMM("SUP"),
  DEF("Deferment Period"),
  DET("Detention"),
  IMP("Imprisonment"),
  IMPRISONMENT("Imprisonment"),
  LIC("Licence"),
  PSYCH("Psychiatric Hospital"),
  SCUS("Custodial Period"),
  SEC104("Breach of supervision requirements"),
  SEC105("Breach due to imprisonable offence"),
  SEC86("Section 86 of 2000 Act"),
  SUP("Sentence Length"),
  SUSP("Suspension Period"),
}
