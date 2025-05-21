package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.RecallType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceTypeClassification

@Entity
@Table(name = "recall_type")
class RecallTypeEntity(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Int,
  @Enumerated(EnumType.STRING)
  val code: RecallType,
  val description: String,
) {
  // TODO RCLL-376 confirm all DPS to NOMIS recall types.
  fun toLegacySentenceType(sentenceType: SentenceTypeEntity): Pair<String, String> = (
    when (this.code) {
      RecallType.LR -> findLicenseRecallLegacySentenceType(sentenceType)
      RecallType.FTR_14 -> "14FTR_ORA"
      RecallType.FTR_28 -> "FTR"
      RecallType.LR_HDC -> "LR"
      RecallType.FTR_HDC_14 -> "14FTRHDC_ORA"
      RecallType.FTR_HDC_28 -> "FTR_HDC"
      RecallType.CUR_HDC -> "CUR"
      RecallType.IN_HDC -> "HDR"
    }
    ) to sentenceType.nomisCjaCode

  private fun findLicenseRecallLegacySentenceType(sentenceType: SentenceTypeEntity): String = when (sentenceType.classification) {
    SentenceTypeClassification.STANDARD -> "LR"
    SentenceTypeClassification.EXTENDED -> {
      if (sentenceType.nomisSentenceCalcType == "LASPO_AR") {
        "LR_LASPO_AR"
      } else if (sentenceType.nomisSentenceCalcType == "LASPO_DR") {
        "LR_LASPO_DR"
      } else {
        "LR_EDS21"
      }
    }
    SentenceTypeClassification.SOPC -> {
      if (sentenceType.nomisSentenceCalcType == "SOPC18") {
        "LR_SOPC18"
      } else {
        "LR_SOPC21"
      }
    }
    else -> throw IllegalStateException("Unknown sentence type classification ${sentenceType.classification} with standard recall")
  }
}
