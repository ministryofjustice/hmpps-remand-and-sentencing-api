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
  fun `recall sentence type mappings`(dpsSentenceType: String, classification: SentenceTypeClassification, recallType: RecallType, expectedLegacySentenceType: String) {
    val sentenceType = SentenceTypeEntity(sentenceTypeUuid = UUID.randomUUID(), description = classification.name, minAgeInclusive = null, maxAgeExclusive = null, minDateInclusive = null, maxDateExclusive = null, minOffenceDateInclusive = null, maxOffenceDateExclusive = null, classification = classification, hintText = null, nomisCjaCode = "", nomisSentenceCalcType = dpsSentenceType, displayOrder = 1, status = ReferenceEntityStatus.ACTIVE)

    val legacySentenceType = RecallTypeEntity(1, recallType, recallType.name).toLegacySentenceType(sentenceType)

    assertThat(legacySentenceType.first).isEqualTo(expectedLegacySentenceType)
  }

  companion object {
    @JvmStatic
    fun nomisToDpsSentenceTypeParameters(): Stream<Arguments> = Stream.of(
      // Standard sentence recall types
      Arguments.of("ADIMP_ORA", SentenceTypeClassification.STANDARD, RecallType.FTR_14, "14FTR_ORA"),
      Arguments.of("ADIMP_ORA", SentenceTypeClassification.STANDARD, RecallType.FTR_28, "FTR"),
      Arguments.of("ADIMP_ORA", SentenceTypeClassification.STANDARD, RecallType.FTR_HDC_14, "14FTRHDC_ORA"),
      Arguments.of("ADIMP_ORA", SentenceTypeClassification.STANDARD, RecallType.FTR_HDC_28, "FTR_HDC"),
      Arguments.of("ADIMP_ORA", SentenceTypeClassification.STANDARD, RecallType.CUR_HDC, "CUR"),
      Arguments.of("ADIMP_ORA", SentenceTypeClassification.STANDARD, RecallType.IN_HDC, "HDR"),
      Arguments.of("ADIMP_ORA", SentenceTypeClassification.STANDARD, RecallType.LR_HDC, "LR"),
      Arguments.of("ADIMP_ORA", SentenceTypeClassification.STANDARD, RecallType.LR, "LR"),

      // Extended sentence recall types
      Arguments.of("EDS18", SentenceTypeClassification.EXTENDED, RecallType.LR, "LR_EDS21"),
      Arguments.of("EDS21", SentenceTypeClassification.EXTENDED, RecallType.LR, "LR_EDS21"),
      Arguments.of("EDSU18", SentenceTypeClassification.EXTENDED, RecallType.LR, "LR_EDS21"),
      Arguments.of("EPP", SentenceTypeClassification.EXTENDED, RecallType.LR, "LR_EDS21"),
      Arguments.of("LASPO_AR", SentenceTypeClassification.EXTENDED, RecallType.LR, "LR_LASPO_AR"),
      Arguments.of("LASPO_DR", SentenceTypeClassification.EXTENDED, RecallType.LR, "LR_LASPO_DR"),
      Arguments.of("STS18", SentenceTypeClassification.EXTENDED, RecallType.LR, "LR_EDS21"),
      Arguments.of("STS21", SentenceTypeClassification.EXTENDED, RecallType.LR, "LR_EDS21"),

      // SOPC recall types
      Arguments.of("SOPC18", SentenceTypeClassification.SOPC, RecallType.LR, "LR_SOPC18"),
      Arguments.of("SOPC21", SentenceTypeClassification.SOPC, RecallType.LR, "LR_SOPC21"),
    )
  }
}
