package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.model
enum class RecallTypeIdentifier(
  val isFixedTermRecall: Boolean = false,
  val lengthInDays: Int?,
) {
  STANDARD_RECALL(false, null),
  STANDARD_RECALL_255(false, null),
  FIXED_TERM_RECALL_14(true, 14),
  FIXED_TERM_RECALL_28(true, 28),
  NONE(false, null),
  ;

  fun toDomain(): RecallType = RecallType(
    isRecall = this == NONE,
    type = this.name,
    isFixedTermRecall = this.isFixedTermRecall,
    lengthInDays = this.lengthInDays ?: 0,
  )

  companion object {
    fun from(value: String?): RecallTypeIdentifier = try {
      if (value.isNullOrBlank()) NONE else valueOf(value)
    } catch (ex: IllegalArgumentException) {
      NONE
    }
  }
}
