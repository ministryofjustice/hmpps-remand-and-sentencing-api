package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum

enum class PeriodLengthEntityStatus {
  ACTIVE,
  DUPLICATE,
  DELETED,
  MANY_CHARGES_DATA_FIX,
  INACTIVE,
  ;

  companion object {
    fun from(sentenceEntityStatus: SentenceEntityStatus): PeriodLengthEntityStatus = when (sentenceEntityStatus) {
      SentenceEntityStatus.MANY_CHARGES_DATA_FIX -> MANY_CHARGES_DATA_FIX
      SentenceEntityStatus.ACTIVE -> ACTIVE
      SentenceEntityStatus.DUPLICATE -> DUPLICATE
      SentenceEntityStatus.DELETED -> DELETED
      SentenceEntityStatus.INACTIVE -> INACTIVE
    }
  }
}
