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
  fun toLegacySentenceType(sentenceType: SentenceTypeEntity): String = toLegacySentenceType(sentenceType.nomisSentenceCalcType, sentenceType.classification)
  fun toLegacySentenceType(sentenceType: String, classification: SentenceTypeClassification): String = when (this.code) {
    RecallType.LR -> findLicenseRecallLegacySentenceType(sentenceType, classification)
    RecallType.FTR_14 -> "14FTR_ORA"
    RecallType.FTR_28 -> if (sentenceType.contains("ORA")) "FTR_ORA" else "FTR"
    RecallType.FTR_56 -> "FTR_56ORA"
    RecallType.FTR_HDC_14 -> "14FTRHDC_ORA"
    RecallType.FTR_HDC_28 -> "FTR_HDC"
    RecallType.CUR_HDC -> if (sentenceType.contains("ORA")) "CUR_ORA" else "CUR"
    RecallType.IN_HDC -> if (sentenceType.contains("ORA")) "HDR_ORA" else "HDR"
  }

  private fun findLicenseRecallLegacySentenceType(sentenceType: String, classification: SentenceTypeClassification): String = when (classification) {
    SentenceTypeClassification.STANDARD -> getStandardRecallType(sentenceType)
    SentenceTypeClassification.EXTENDED -> getExtendedRecallType(sentenceType)
    SentenceTypeClassification.SOPC -> getSopcExtendedRecallType(sentenceType)
    SentenceTypeClassification.INDETERMINATE -> getIndeterminateRecallType(sentenceType)
    else -> "LR"
  }

  private fun getIndeterminateRecallType(sentenceType: String): String = when (sentenceType) {
    "ALP" -> "LR_ALP"
    "ALP_CODE18" -> "LR_ALP_CDE18"
    "ALP_CODE21" -> "LR_ALP_CDE21"
    "ALP_LASPO" -> "LR_ALP_LASPO"
    "DLP" -> "LR_DLP"
    "IPP" -> "LR_IPP"
    "MLP" -> "LR_MLP"
    "LR_ALP" -> "LR_ALP"
    "LR_ALP_CODE18" -> "LR_ALP_CDE18"
    "LR_ALP_CDE21" -> "LR_ALP_CDE21"
    "LR_ALP_LASPO" -> "LR_ALP_LASPO"
    "LR_DLP" -> "LR_DLP"
    "LR_IPP" -> "LR_IPP"
    "LR_MLP" -> "LR_MLP"
    else -> "LR_LIFE"
  }

  private fun getSopcExtendedRecallType(sentenceType: String): String = when (sentenceType) {
    "SEC236A" -> "LR_SEC236A"
    "SOPC18" -> "LR_SOPC18"
    "SOPC21" -> "LR_SOPC21"
    "SDOPCU18" -> "LR_SOPC18"
    "LR_SEC236A" -> "LR_SEC236A"
    "LR_SOPC18" -> "LR_SOPC18"
    "LR_SOPC21" -> "LR_SOPC21"
    else -> "LR_SOPC21"
  }

  private fun getExtendedRecallType(sentenceType: String): String = when (sentenceType) {
    "LASPO_AR" -> "LR_LASPO_AR"
    "LASPO_DR" -> "LR_LASPO_DR"
    "EDS18" -> "LR_EDS18"
    "EDS21" -> "LR_EDS21"
    "EDSU18" -> "LR_EDSU18"
    "EPP" -> "LR_EPP"
    "EXT" -> "LR"
    "STS18" -> "LR"
    "STS21" -> "LR"
    "LR_LASPO_AR" -> "LR_LASPO_AR"
    "LR_LASPO_DR" -> "LR_LASPO_DR"
    "LR_EDS18" -> "LR_EDS18"
    "LR_EDS21" -> "LR_EDS21"
    "LR_EDSU18" -> "LR_EDSU18"
    "LR_EPP" -> "LR_EPP"
    else -> "LR_EDS21"
  }

  private fun getStandardRecallType(sentenceType: String): String = when (sentenceType) {
    "ADIMP" -> "LR"
    "ADIMP_ORA" -> "LR_ORA"
    "SEC250" -> "LRSEC250_ORA"
    "SEC250_ORA" -> "LRSEC250_ORA"
    "YOI" -> "LR"
    "YOI_ORA" -> "LR_YOI_ORA"
    "LRSEC250_ORA" -> "LRSEC250_ORA"
    "LR_YOI_ORA" -> "LR_YOI_ORA"
    else -> "LR"
  }
}
