package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@Schema(
  description = "Recall type including name, length in days (if fixed term), and a fixed-term indicator",
  example = """{"name": "FIXED_TERM_RECALL_14", "lengthInDays": 14, "isFixedTermRecall": true}""",
)
enum class RecallType(
  @get:Schema(description = "Recall length in days", example = "14")
  val lengthInDays: Int? = null,

  @get:Schema(description = "Whether this recall is a fixed term", example = "true")
  val isFixedTermRecall: Boolean = false,
) {
  STANDARD_RECALL,
  STANDARD_RECALL_255,
  FIXED_TERM_RECALL_14(14, true),
  FIXED_TERM_RECALL_28(28, true),
  NONE,
  ;

  companion object {
    @JsonCreator
    @JvmStatic
    fun fromJson(@JsonProperty("name") name: String?): RecallType = name?.let { entries.firstOrNull { it.name == name } } ?: NONE

    fun from(classification: String): RecallType = entries.firstOrNull { it.name == classification } ?: NONE
  }

  @JsonProperty("name")
  fun getName(): String = name
}
