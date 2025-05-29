package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.sentence

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.RecallTypeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceTypeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.RecallType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ReferenceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceTypeClassification
import java.util.UUID
import java.util.stream.Stream

class LegacyGetSentenceSentenceTypeTests {

  @ParameterizedTest(name = "DPS to NOMIS sentence type, original sentence type {0} and classification {1} recall type {2} will result in legacy recall sentence type {3}")
  @MethodSource("nomisToDpsSentenceTypeParameters")
  fun `recall sentence type mappings`(
    nomisCjaCode: String,
    nomisSentenceCalcType: String,
    classification: SentenceTypeClassification,
    recallType: RecallType,
    expectedLegacySentenceType: String,
    expectedCategory: String,
  ) {
    val sentenceType = SentenceTypeEntity(
      sentenceTypeUuid = UUID.randomUUID(),
      description = classification.name,
      minAgeInclusive = null,
      maxAgeExclusive = null,
      minDateInclusive = null,
      maxDateExclusive = null,
      minOffenceDateInclusive = null,
      maxOffenceDateExclusive = null,
      classification = classification,
      hintText = null,
      nomisCjaCode = nomisCjaCode,
      nomisSentenceCalcType = nomisSentenceCalcType,
      displayOrder = 1,
      status = ReferenceEntityStatus.ACTIVE,
    )

    val legacySentenceType = RecallTypeEntity(1, recallType, recallType.name).toLegacySentenceType(sentenceType)

    assertThat(legacySentenceType.first).isEqualTo(expectedLegacySentenceType)
    assertThat(legacySentenceType.second).isEqualTo(expectedCategory)
  }

  companion object {
    @JvmStatic
    fun nomisToDpsSentenceTypeParameters(): Stream<Arguments> = Stream.of(
      // Non-standard recalls
      Arguments.of("2020", "ADIMP_ORA", SentenceTypeClassification.STANDARD, RecallType.FTR_14, "14FTR_ORA", "2020"),
      Arguments.of("2020", "ADIMP_ORA", SentenceTypeClassification.STANDARD, RecallType.FTR_28, "FTR_ORA", "2020"),
      Arguments.of("2020", "ADIMP", SentenceTypeClassification.STANDARD, RecallType.FTR_28, "FTR", "2020"),
      Arguments.of(
        "2020",
        "ADIMP_ORA",
        SentenceTypeClassification.STANDARD,
        RecallType.FTR_HDC_14,
        "14FTRHDC_ORA",
        "2020",
      ),
      Arguments.of("2020", "ADIMP_ORA", SentenceTypeClassification.STANDARD, RecallType.FTR_HDC_28, "FTR_HDC", "2020"),
      Arguments.of("2020", "ADIMP", SentenceTypeClassification.STANDARD, RecallType.FTR_HDC_28, "FTR_HDC", "2020"),
      Arguments.of("2020", "ADIMP_ORA", SentenceTypeClassification.STANDARD, RecallType.CUR_HDC, "CUR_ORA", "2020"),
      Arguments.of("2020", "ADIMP_ORA", SentenceTypeClassification.STANDARD, RecallType.IN_HDC, "HDR_ORA", "2020"),
      Arguments.of("2020", "ADIMP", SentenceTypeClassification.STANDARD, RecallType.CUR_HDC, "CUR", "2020"),
      Arguments.of("2020", "ADIMP", SentenceTypeClassification.STANDARD, RecallType.IN_HDC, "HDR", "2020"),
      Arguments.of("2020", "ADIMP_ORA", SentenceTypeClassification.STANDARD, RecallType.LR_HDC, "LR", "2020"),

      // Standard recalls
      // Standard sentence types
      Arguments.of("2020", "ADIMP_ORA", SentenceTypeClassification.STANDARD, RecallType.LR, "LR_ORA", "2020"),
      Arguments.of("2020", "ADIMP", SentenceTypeClassification.STANDARD, RecallType.LR, "LR", "2020"),
      Arguments.of("2020", "SEC250", SentenceTypeClassification.STANDARD, RecallType.LR, "LRSEC250_ORA", "2020"),
      Arguments.of("2020", "SEC250_ORA", SentenceTypeClassification.STANDARD, RecallType.LR, "LRSEC250_ORA", "2020"),
      Arguments.of("2020", "YOI", SentenceTypeClassification.STANDARD, RecallType.LR, "LR", "2020"),
      Arguments.of("2020", "YOI_ORA", SentenceTypeClassification.STANDARD, RecallType.LR, "LR_YOI_ORA", "2020"),
      Arguments.of("2003", "YOI", SentenceTypeClassification.STANDARD, RecallType.LR, "LR", "2003"),
      Arguments.of("2003", "YOI_ORA", SentenceTypeClassification.STANDARD, RecallType.LR, "LR_YOI_ORA", "2003"),

      // Extended sentence types
      Arguments.of("2020", "EDS18", SentenceTypeClassification.EXTENDED, RecallType.LR, "LR_EDS18", "2020"),
      Arguments.of("2020", "EDS21", SentenceTypeClassification.EXTENDED, RecallType.LR, "LR_EDS21", "2020"),
      Arguments.of("2020", "EDSU18", SentenceTypeClassification.EXTENDED, RecallType.LR, "LR_EDSU18", "2020"),
      Arguments.of("2003", "EPP", SentenceTypeClassification.EXTENDED, RecallType.LR, "LR_EPP", "2003"),
      Arguments.of("2003", "LASPO_AR", SentenceTypeClassification.EXTENDED, RecallType.LR, "LR_LASPO_AR", "2003"),
      Arguments.of("2003", "LASPO_DR", SentenceTypeClassification.EXTENDED, RecallType.LR, "LR_LASPO_DR", "2003"),
      Arguments.of("2020", "STS18", SentenceTypeClassification.EXTENDED, RecallType.LR, "LR", "2020"),
      Arguments.of("2020", "STS21", SentenceTypeClassification.EXTENDED, RecallType.LR, "LR", "2020"),

      // SOPC sentence types
      Arguments.of("2020", "SOPC18", SentenceTypeClassification.SOPC, RecallType.LR, "LR_SOPC18", "2020"),
      Arguments.of("2020", "SOPC21", SentenceTypeClassification.SOPC, RecallType.LR, "LR_SOPC21", "2020"),
      Arguments.of("2003", "SEC236A", SentenceTypeClassification.SOPC, RecallType.LR, "LR_SEC236A", "2003"),
      Arguments.of("2020", "SDOPCU18", SentenceTypeClassification.SOPC, RecallType.LR, "LR_SOPC18", "2020"),

      // Indeterminate sentence types.
      Arguments.of("2020", "ALP", SentenceTypeClassification.INDETERMINATE, RecallType.LR, "LR_ALP", "2020"),
      Arguments.of(
        "2020",
        "ALP_CODE18",
        SentenceTypeClassification.INDETERMINATE,
        RecallType.LR,
        "LR_ALP_CDE18",
        "2020",
      ),
      Arguments.of(
        "2020",
        "ALP_CODE21",
        SentenceTypeClassification.INDETERMINATE,
        RecallType.LR,
        "LR_ALP_CDE21",
        "2020",
      ),
      Arguments.of(
        "2003",
        "ALP_LASPO",
        SentenceTypeClassification.INDETERMINATE,
        RecallType.LR,
        "LR_ALP_LASPO",
        "2003",
      ),
      Arguments.of("2020", "DFL", SentenceTypeClassification.INDETERMINATE, RecallType.LR, "LR_LIFE", "2020"),
      Arguments.of("2020", "DLP", SentenceTypeClassification.INDETERMINATE, RecallType.LR, "LR_DLP", "2020"),
      Arguments.of("2003", "IPP", SentenceTypeClassification.INDETERMINATE, RecallType.LR, "LR_IPP", "2003"),
      Arguments.of("1991", "LIFE", SentenceTypeClassification.INDETERMINATE, RecallType.LR, "LR_LIFE", "1991"),
      Arguments.of("2020", "MLP", SentenceTypeClassification.INDETERMINATE, RecallType.LR, "LR_MLP", "2020"),
      Arguments.of("2020", "SEC272", SentenceTypeClassification.INDETERMINATE, RecallType.LR, "LR_LIFE", "2020"),
      Arguments.of("2020", "SEC275", SentenceTypeClassification.INDETERMINATE, RecallType.LR, "LR_LIFE", "2020"),
      Arguments.of("1991", "SEC93", SentenceTypeClassification.INDETERMINATE, RecallType.LR, "LR_LIFE", "1991"),
      Arguments.of("2003", "SEC93_03", SentenceTypeClassification.INDETERMINATE, RecallType.LR, "LR_LIFE", "2003"),
      Arguments.of("2003", "SEC94", SentenceTypeClassification.INDETERMINATE, RecallType.LR, "LR_LIFE", "2003"),
    )
  }
}
