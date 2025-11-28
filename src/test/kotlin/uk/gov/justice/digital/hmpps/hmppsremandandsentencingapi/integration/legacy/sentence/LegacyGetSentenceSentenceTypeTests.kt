package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.sentence

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.RecallTypeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.RecallType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceTypeClassification
import java.util.stream.Stream

class LegacyGetSentenceSentenceTypeTests {

  @ParameterizedTest(name = "DPS to NOMIS sentence type, original sentence type {0} and classification {1} recall type {2} will result in legacy recall sentence type {3}")
  @MethodSource("nomisToDpsSentenceTypeParameters")
  fun `recall sentence type mappings`(
    nomisSentenceCalcType: String,
    classification: SentenceTypeClassification,
    recallType: RecallType,
    expectedLegacySentenceType: String,
  ) {
    val legacySentenceType = RecallTypeEntity(1, recallType, recallType.name).toLegacySentenceType(nomisSentenceCalcType, classification)

    assertThat(legacySentenceType).isEqualTo(expectedLegacySentenceType)
  }

  companion object {
    @JvmStatic
    fun nomisToDpsSentenceTypeParameters(): Stream<Arguments> = Stream.of(
      // Non-standard recalls
      Arguments.of("ADIMP_ORA", SentenceTypeClassification.STANDARD, RecallType.FTR_14, "14FTR_ORA"),
      Arguments.of("ADIMP_ORA", SentenceTypeClassification.STANDARD, RecallType.FTR_28, "FTR_ORA"),
      Arguments.of("ADIMP", SentenceTypeClassification.STANDARD, RecallType.FTR_28, "FTR"),
      Arguments.of(

        "ADIMP_ORA",
        SentenceTypeClassification.STANDARD,
        RecallType.FTR_HDC_14,
        "14FTRHDC_ORA",

      ),
      Arguments.of("ADIMP_ORA", SentenceTypeClassification.STANDARD, RecallType.FTR_HDC_28, "FTR_HDC"),
      Arguments.of("ADIMP", SentenceTypeClassification.STANDARD, RecallType.FTR_HDC_28, "FTR_HDC"),
      Arguments.of("ADIMP_ORA", SentenceTypeClassification.STANDARD, RecallType.CUR_HDC, "CUR_ORA"),
      Arguments.of("ADIMP_ORA", SentenceTypeClassification.STANDARD, RecallType.IN_HDC, "HDR_ORA"),
      Arguments.of("ADIMP", SentenceTypeClassification.STANDARD, RecallType.CUR_HDC, "CUR"),
      Arguments.of("ADIMP", SentenceTypeClassification.STANDARD, RecallType.IN_HDC, "HDR"),

      // Standard recalls
      // Standard sentence types
      Arguments.of("ADIMP_ORA", SentenceTypeClassification.STANDARD, RecallType.LR, "LR_ORA"),
      Arguments.of("ADIMP", SentenceTypeClassification.STANDARD, RecallType.LR, "LR"),
      Arguments.of("SEC250", SentenceTypeClassification.STANDARD, RecallType.LR, "LRSEC250_ORA"),
      Arguments.of("SEC250_ORA", SentenceTypeClassification.STANDARD, RecallType.LR, "LRSEC250_ORA"),
      Arguments.of("YOI", SentenceTypeClassification.STANDARD, RecallType.LR, "LR"),
      Arguments.of("YOI_ORA", SentenceTypeClassification.STANDARD, RecallType.LR, "LR_YOI_ORA"),
      Arguments.of("YOI", SentenceTypeClassification.STANDARD, RecallType.LR, "LR"),
      Arguments.of("YOI_ORA", SentenceTypeClassification.STANDARD, RecallType.LR, "LR_YOI_ORA"),

      // Extended sentence types
      Arguments.of("EDS18", SentenceTypeClassification.EXTENDED, RecallType.LR, "LR_EDS18"),
      Arguments.of("EDS21", SentenceTypeClassification.EXTENDED, RecallType.LR, "LR_EDS21"),
      Arguments.of("EDSU18", SentenceTypeClassification.EXTENDED, RecallType.LR, "LR_EDSU18"),
      Arguments.of("EPP", SentenceTypeClassification.EXTENDED, RecallType.LR, "LR_EPP"),
      Arguments.of("LASPO_AR", SentenceTypeClassification.EXTENDED, RecallType.LR, "LR_LASPO_AR"),
      Arguments.of("LASPO_DR", SentenceTypeClassification.EXTENDED, RecallType.LR, "LR_LASPO_DR"),
      Arguments.of("STS18", SentenceTypeClassification.EXTENDED, RecallType.LR, "LR"),
      Arguments.of("STS21", SentenceTypeClassification.EXTENDED, RecallType.LR, "LR"),

      // SOPC sentence types
      Arguments.of("SOPC18", SentenceTypeClassification.SOPC, RecallType.LR, "LR_SOPC18"),
      Arguments.of("SOPC21", SentenceTypeClassification.SOPC, RecallType.LR, "LR_SOPC21"),
      Arguments.of("SEC236A", SentenceTypeClassification.SOPC, RecallType.LR, "LR_SEC236A"),
      Arguments.of("SDOPCU18", SentenceTypeClassification.SOPC, RecallType.LR, "LR_SOPC18"),

      // Indeterminate sentence types.
      Arguments.of("ALP", SentenceTypeClassification.INDETERMINATE, RecallType.LR, "LR_ALP"),
      Arguments.of(
        "ALP_CODE18",
        SentenceTypeClassification.INDETERMINATE,
        RecallType.LR,
        "LR_ALP_CDE18",
        "2020",
      ),
      Arguments.of(
        "ALP_CODE21",
        SentenceTypeClassification.INDETERMINATE,
        RecallType.LR,
        "LR_ALP_CDE21",
        "2020",
      ),
      Arguments.of(
        "ALP_LASPO",
        SentenceTypeClassification.INDETERMINATE,
        RecallType.LR,
        "LR_ALP_LASPO",

      ),
      Arguments.of("DFL", SentenceTypeClassification.INDETERMINATE, RecallType.LR, "LR_LIFE"),
      Arguments.of("DLP", SentenceTypeClassification.INDETERMINATE, RecallType.LR, "LR_DLP"),
      Arguments.of("IPP", SentenceTypeClassification.INDETERMINATE, RecallType.LR, "LR_IPP"),
      Arguments.of("LIFE", SentenceTypeClassification.INDETERMINATE, RecallType.LR, "LR_LIFE"),
      Arguments.of("MLP", SentenceTypeClassification.INDETERMINATE, RecallType.LR, "LR_MLP"),
      Arguments.of("SEC272", SentenceTypeClassification.INDETERMINATE, RecallType.LR, "LR_LIFE"),
      Arguments.of("SEC275", SentenceTypeClassification.INDETERMINATE, RecallType.LR, "LR_LIFE"),
      Arguments.of("SEC93", SentenceTypeClassification.INDETERMINATE, RecallType.LR, "LR_LIFE"),
      Arguments.of("SEC93_03", SentenceTypeClassification.INDETERMINATE, RecallType.LR, "LR_LIFE"),
      Arguments.of("SEC94", SentenceTypeClassification.INDETERMINATE, RecallType.LR, "LR_LIFE"),

      // Legacy recall types, with new DPS recall.
      Arguments.of("14FTR_ORA", SentenceTypeClassification.STANDARD, RecallType.LR, "LR"),
      Arguments.of("14FTRHDC_ORA", SentenceTypeClassification.STANDARD, RecallType.LR, "LR"),
      Arguments.of("LRSEC250_ORA", SentenceTypeClassification.STANDARD, RecallType.LR, "LRSEC250_ORA"),
      Arguments.of("LR_YOI_ORA", SentenceTypeClassification.STANDARD, RecallType.LR, "LR_YOI_ORA"),
      Arguments.of("14FTR_ORA", SentenceTypeClassification.STANDARD, RecallType.FTR_28, "FTR_ORA"),

      Arguments.of("LR_SEC236A", SentenceTypeClassification.SOPC, RecallType.LR, "LR_SEC236A"),

    )
  }
}
