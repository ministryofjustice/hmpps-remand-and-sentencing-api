package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
enum class RecallType(
  val lengthInDays: Int? = null,
  val isFixedTermRecall: Boolean = false,
) {
  STANDARD_RECALL(),
  STANDARD_RECALL_255(),
  FIXED_TERM_RECALL_14(14, true),
  FIXED_TERM_RECALL_28(28, true),
  NONE,
  ;

  companion object {
    @JsonCreator
    @JvmStatic
    fun fromJson(@JsonProperty("name") name: String?): RecallType = name?.let { entries.firstOrNull { it.name == name } } ?: NONE
    fun from(classification: String): RecallType = RecallType.entries.firstOrNull { it.name == classification } ?: NONE
  }

  @JsonProperty("name")
  fun getName(): String = name
}
