package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.LegacySentenceType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.LegacySentenceTypeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceTypeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ReferenceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceTypeClassification
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceTypePeriodDefinition
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.LegacySentenceTypeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.SentenceTypeRepository
import java.time.LocalDate
import java.util.UUID

class SentenceTypesServiceTests {

  private val sentenceTypeRepository = mockk<SentenceTypeRepository>()
  private val legacySentenceTypeRepository = mockk<LegacySentenceTypeRepository>()
  private val objectMapper = ObjectMapper()

  private val sentenceTypeService = SentenceTypeService(
    sentenceTypeRepository,
    legacySentenceTypeRepository,
    objectMapper,
  )

  @Test
  fun `getLegacySentenceTypeData returns correct data when legacy entity is found`() {
    val legacyKey = "TEST_KEY"
    val testUuid = UUID.randomUUID()

    val sentenceTypeEntity = SentenceTypeEntity(
      sentenceTypeUuid = testUuid,
      id = 1,
      description = "",
      minAgeInclusive = null,
      maxAgeExclusive = null,
      minDateInclusive = null,
      maxDateExclusive = null,
      maxOffenceDateExclusive = null,
      minOffenceDateInclusive = null,
      classification = SentenceTypeClassification.STANDARD,
      hintText = "",
      nomisCjaCode = "ADIMP_ORA",
      nomisSentenceCalcType = "IMP",
      displayOrder = 5432,
      status = ReferenceEntityStatus.ACTIVE,
    )

    val legacySentenceTypeEntity = LegacySentenceTypeEntity(
      id = 1,
      nomisSentenceTypeReference = "calcType1",
      classification = "STANDARD",
      sentencingAct = 2002,
      nomisActive = true,
      nomisExpiryDate = LocalDate.of(2025, 1, 1),
      nomisDescription = "Test description",
      eligibility = "{\"toreraEligibilityType\": \"SDS\", \"sdsPlusEligibilityType\": \"SDS\"}",
      recallType = null,
      sentenceType = sentenceTypeEntity,
      nomisTerms = "[\"Imprisonment\"]",
    )
    every { legacySentenceTypeRepository.findByNomisSentenceTypeReference(legacyKey) } returns listOf(legacySentenceTypeEntity)

    val result: List<LegacySentenceType> = sentenceTypeService.getLegacySentencesByNomisSentenceTypeReference(legacyKey)

    Assertions.assertEquals(1, result.size)
    val dto = result.first()
    Assertions.assertEquals(legacySentenceTypeEntity.id, dto.id)
    Assertions.assertEquals(legacySentenceTypeEntity.nomisSentenceTypeReference, dto.nomisSentenceTypeReference)
    Assertions.assertEquals(SentenceTypeClassification.STANDARD, dto.classification)

    val expectedPeriodDefinition = SentenceTypePeriodDefinition.mapping[SentenceTypeClassification.STANDARD]
    Assertions.assertEquals(expectedPeriodDefinition, dto.classificationPeriodDefinition)
    Assertions.assertEquals(legacySentenceTypeEntity.sentencingAct, dto.sentencingAct)
    Assertions.assertEquals(legacySentenceTypeEntity.nomisActive, dto.nomisActive)
    Assertions.assertEquals(legacySentenceTypeEntity.nomisExpiryDate, dto.nomisExpiryDate)
    Assertions.assertEquals(legacySentenceTypeEntity.nomisDescription, dto.nomisDescription)
    Assertions.assertNotNull(dto.eligibility)
    Assertions.assertNull(dto.recallType)
  }
}
