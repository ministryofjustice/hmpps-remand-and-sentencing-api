package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum

data class PeriodLengthDetail(
  val years: String,
  val periodOrder: List<String>,
  val periodLengthType: PeriodLengthType,
  val description: String,
)

data class Period(
  val type: PeriodLengthType,
  val auto: Boolean = false,
  val periodLength: PeriodLengthDetail? = null,
)

data class SentenceTypePeriodDefinitions(
  val periodDefinitions: List<Period>,
)

object SentenceTypePeriodDefinition {
  val mapping: Map<SentenceTypeClassification, SentenceTypePeriodDefinitions> = mapOf(
    SentenceTypeClassification.STANDARD to SentenceTypePeriodDefinitions(
      periodDefinitions = listOf(
        Period(type = PeriodLengthType.SENTENCE_LENGTH),
      ),
    ),
    SentenceTypeClassification.EXTENDED to SentenceTypePeriodDefinitions(
      periodDefinitions = listOf(
        Period(type = PeriodLengthType.CUSTODIAL_TERM),
        Period(type = PeriodLengthType.LICENCE_PERIOD),
      ),
    ),
    SentenceTypeClassification.SOPC to SentenceTypePeriodDefinitions(
      periodDefinitions = listOf(
        Period(type = PeriodLengthType.SENTENCE_LENGTH),
        Period(
          type = PeriodLengthType.LICENCE_PERIOD,
          auto = true,
          periodLength = PeriodLengthDetail(
            years = "1",
            periodOrder = listOf("years"),
            periodLengthType = PeriodLengthType.LICENCE_PERIOD,
            description = "Licence period",
          ),
        ),
      ),
    ),
    SentenceTypeClassification.INDETERMINATE to SentenceTypePeriodDefinitions(
      periodDefinitions = listOf(
        Period(type = PeriodLengthType.TARIFF_LENGTH),
      ),
    ),
    SentenceTypeClassification.BOTUS to SentenceTypePeriodDefinitions(
      periodDefinitions = listOf(
        Period(type = PeriodLengthType.TERM_LENGTH),
      ),
    ),
    SentenceTypeClassification.CIVIL to SentenceTypePeriodDefinitions(
      periodDefinitions = listOf(
        Period(type = PeriodLengthType.TERM_LENGTH),
      ),
    ),
    SentenceTypeClassification.DTO to SentenceTypePeriodDefinitions(
      periodDefinitions = listOf(
        Period(type = PeriodLengthType.TERM_LENGTH),
      ),
    ),
    SentenceTypeClassification.FINE to SentenceTypePeriodDefinitions(
      periodDefinitions = listOf(
        Period(type = PeriodLengthType.TERM_LENGTH),
      ),
    ),
    SentenceTypeClassification.LEGACY to SentenceTypePeriodDefinitions(
      periodDefinitions = listOf(
        Period(type = PeriodLengthType.SENTENCE_LENGTH),
      ),
    ),
    SentenceTypeClassification.NON_CUSTODIAL to SentenceTypePeriodDefinitions(
      periodDefinitions = emptyList(),
    ),
  )
}
