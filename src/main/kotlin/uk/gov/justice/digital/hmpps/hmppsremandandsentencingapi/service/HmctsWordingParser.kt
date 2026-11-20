package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

class HmctsWordingParser(
  val wording: String,
) {

  fun parseSuspendedSentenceOffenceCode(): String? = SUSPENDED_SENTENCE_OFFENCE_CODE_REGEX.find(wording)?.groupValues?.get(1)

  companion object {
    val SUSPENDED_SENTENCE_OFFENCE_CODE_REGEX = Regex("""Original code\s*:\s*([A-Z]{2}\d+)""")
  }
}
