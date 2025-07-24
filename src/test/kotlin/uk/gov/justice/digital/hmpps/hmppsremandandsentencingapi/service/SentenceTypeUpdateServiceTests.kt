package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.SentenceTypeUpdate
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.UpdateSentenceTypeRequest
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtCaseEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceTypeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ReferenceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceTypeClassification
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtCaseRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.SentenceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.SentenceTypeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.SentenceHistoryRepository
import java.util.UUID

class SentenceTypeUpdateServiceTests {
  private val courtCaseRepository = mockk<CourtCaseRepository>()
  private val sentenceTypeRepository = mockk<SentenceTypeRepository>()
  private val sentenceHistoryRepository = mockk<SentenceHistoryRepository>()
  private val serviceUserService = mockk<ServiceUserService>()

  private lateinit var sentenceTypeUpdateService: SentenceTypeUpdateService

  private val unknownPreRecallSentenceTypeUuid = UUID.fromString("f9a1551e-86b1-425b-96f7-23465a0f05fc")
  private val courtCaseUuid = UUID.randomUUID()
  private val sentenceUuid = UUID.randomUUID()
  private val sdsTypeUuid = UUID.randomUUID()

  @BeforeEach
  fun setUp() {
    sentenceTypeUpdateService = SentenceTypeUpdateService(
      courtCaseRepository,
      sentenceTypeRepository,
      sentenceHistoryRepository,
      serviceUserService,
    )

    every { serviceUserService.getUsername() } returns "test-user"
    every { sentenceHistoryRepository.save(any()) } returnsArgument 0
  }

  @Test
  fun `successfully update sentence type`() {
    // Given
    val courtCase = createCourtCase()
    val sentence = createSentence(unknownPreRecallSentenceTypeUuid)
    val newSentenceType = createSentenceType(sdsTypeUuid, "SDS")

    every { courtCaseRepository.findByCaseUniqueIdentifier(courtCaseUuid.toString()) } returns courtCase
    every { courtCaseRepository.findSentencesByCourtCaseUuid(courtCaseUuid.toString()) } returns listOf(sentence)
    every { sentenceTypeRepository.findBySentenceTypeUuidIn(listOf(sdsTypeUuid)) } returns listOf(newSentenceType)

    val request = UpdateSentenceTypeRequest(
      updates = listOf(
        SentenceTypeUpdate(
          sentenceUuid = sentenceUuid,
          sentenceType = sdsTypeUuid.toString(),
        ),
      ),
    )

    // When
    val response = sentenceTypeUpdateService.updateSentenceTypes(courtCaseUuid, request)

    // Then
    assertThat(response.updatedCount).isEqualTo(1)
    assertThat(response.updates).hasSize(1)
    assertThat(response.updates[0].sentenceUuid).isEqualTo(sentenceUuid)
    assertThat(response.updates[0].sentenceType).isEqualTo(sdsTypeUuid.toString())

    verify { sentenceHistoryRepository.save(any()) }
  }

  @Test
  fun `throw EntityNotFoundException when court case not found`() {
    // Given
    every { courtCaseRepository.findByCaseUniqueIdentifier(courtCaseUuid.toString()) } returns null

    val request = UpdateSentenceTypeRequest(
      updates = listOf(
        SentenceTypeUpdate(
          sentenceUuid = sentenceUuid,
          sentenceType = sdsTypeUuid.toString(),
        ),
      ),
    )

    // When/Then
    assertThatThrownBy {
      sentenceTypeUpdateService.updateSentenceTypes(courtCaseUuid, request)
    }
      .isInstanceOf(EntityNotFoundException::class.java)
      .hasMessage("Court case with UUID $courtCaseUuid not found")
  }

  @Test
  fun `throw IllegalStateException when court case is deleted`() {
    // Given
    val courtCase = createCourtCase(status = EntityStatus.DELETED)
    every { courtCaseRepository.findByCaseUniqueIdentifier(courtCaseUuid.toString()) } returns courtCase

    val request = UpdateSentenceTypeRequest(
      updates = listOf(
        SentenceTypeUpdate(
          sentenceUuid = sentenceUuid,
          sentenceType = sdsTypeUuid.toString(),
        ),
      ),
    )

    // When/Then
    assertThatThrownBy {
      sentenceTypeUpdateService.updateSentenceTypes(courtCaseUuid, request)
    }
      .isInstanceOf(IllegalStateException::class.java)
      .hasMessage("Court case with UUID $courtCaseUuid is deleted")
  }

  @Test
  fun `throw EntityNotFoundException when sentence not found in court case`() {
    // Given
    val courtCase = createCourtCase()
    every { courtCaseRepository.findByCaseUniqueIdentifier(courtCaseUuid.toString()) } returns courtCase
    every { courtCaseRepository.findSentencesByCourtCaseUuid(courtCaseUuid.toString()) } returns emptyList()
    every { sentenceTypeRepository.findBySentenceTypeUuidIn(any()) } returns emptyList()

    val request = UpdateSentenceTypeRequest(
      updates = listOf(
        SentenceTypeUpdate(
          sentenceUuid = sentenceUuid,
          sentenceType = sdsTypeUuid.toString(),
        ),
      ),
    )

    // When/Then
    assertThatThrownBy {
      sentenceTypeUpdateService.updateSentenceTypes(courtCaseUuid, request)
    }
      .isInstanceOf(EntityNotFoundException::class.java)
      .hasMessage("Sentence with UUID $sentenceUuid not found in court case")
  }

  @Test
  fun `throw IllegalArgumentException when sentence does not have unknown pre-recall sentence type`() {
    // Given
    val courtCase = createCourtCase()
    val sentence = createSentence(sdsTypeUuid) // Different type

    every { courtCaseRepository.findByCaseUniqueIdentifier(courtCaseUuid.toString()) } returns courtCase
    every { courtCaseRepository.findSentencesByCourtCaseUuid(courtCaseUuid.toString()) } returns listOf(sentence)
    every { sentenceTypeRepository.findBySentenceTypeUuidIn(any()) } returns emptyList()

    val request = UpdateSentenceTypeRequest(
      updates = listOf(
        SentenceTypeUpdate(
          sentenceUuid = sentenceUuid,
          sentenceType = sdsTypeUuid.toString(),
        ),
      ),
    )

    // When/Then
    assertThatThrownBy {
      sentenceTypeUpdateService.updateSentenceTypes(courtCaseUuid, request)
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Sentence $sentenceUuid does not have type 'unknown pre-recall sentence'")
  }

  @Test
  fun `throw EntityNotFoundException when new sentence type not found`() {
    // Given
    val courtCase = createCourtCase()
    val sentence = createSentence(unknownPreRecallSentenceTypeUuid)

    every { courtCaseRepository.findByCaseUniqueIdentifier(courtCaseUuid.toString()) } returns courtCase
    every { courtCaseRepository.findSentencesByCourtCaseUuid(courtCaseUuid.toString()) } returns listOf(sentence)
    every { sentenceTypeRepository.findBySentenceTypeUuidIn(listOf(sdsTypeUuid)) } returns emptyList()

    val request = UpdateSentenceTypeRequest(
      updates = listOf(
        SentenceTypeUpdate(
          sentenceUuid = sentenceUuid,
          sentenceType = sdsTypeUuid.toString(),
        ),
      ),
    )

    // When/Then
    assertThatThrownBy {
      sentenceTypeUpdateService.updateSentenceTypes(courtCaseUuid, request)
    }
      .isInstanceOf(EntityNotFoundException::class.java)
      .hasMessage("Sentence type '$sdsTypeUuid' is not a valid sentence type")
  }

  @Test
  fun `throw IllegalArgumentException when request exceeds maximum updates`() {
    // Given
    val updates = (1..51).map {
      SentenceTypeUpdate(
        sentenceUuid = UUID.randomUUID(),
        sentenceType = UUID.randomUUID().toString(),
      )
    }

    val request = UpdateSentenceTypeRequest(updates = updates)

    // When/Then
    assertThatThrownBy {
      sentenceTypeUpdateService.updateSentenceTypes(courtCaseUuid, request)
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Maximum of 50 sentence updates allowed per request")
  }

  @Test
  fun `successfully update multiple sentences`() {
    // Given
    val courtCase = createCourtCase()
    val sentence1 = createSentence(unknownPreRecallSentenceTypeUuid, UUID.randomUUID())
    val sentence2 = createSentence(unknownPreRecallSentenceTypeUuid, UUID.randomUUID())
    val newSentenceType = createSentenceType(sdsTypeUuid, "SDS")

    every { courtCaseRepository.findByCaseUniqueIdentifier(courtCaseUuid.toString()) } returns courtCase
    every { courtCaseRepository.findSentencesByCourtCaseUuid(courtCaseUuid.toString()) } returns listOf(sentence1, sentence2)
    every { sentenceTypeRepository.findBySentenceTypeUuidIn(listOf(sdsTypeUuid)) } returns listOf(newSentenceType)

    val request = UpdateSentenceTypeRequest(
      updates = listOf(
        SentenceTypeUpdate(
          sentenceUuid = sentence1.sentenceUuid,
          sentenceType = sdsTypeUuid.toString(),
        ),
        SentenceTypeUpdate(
          sentenceUuid = sentence2.sentenceUuid,
          sentenceType = sdsTypeUuid.toString(),
        ),
      ),
    )

    // When
    val response = sentenceTypeUpdateService.updateSentenceTypes(courtCaseUuid, request)

    // Then
    assertThat(response.updatedCount).isEqualTo(2)
    assertThat(response.updates).hasSize(2)
    verify(exactly = 2) { sentenceHistoryRepository.save(any()) }
  }

  private fun createCourtCase(status: EntityStatus = EntityStatus.ACTIVE): CourtCaseEntity = mockk<CourtCaseEntity>().also { caseEntity ->
    every { caseEntity.statusId } returns status
    every { caseEntity.caseUniqueIdentifier } returns courtCaseUuid.toString()
    every { caseEntity.prisonerId } returns "PRISONER1"
    every { caseEntity.id } returns 1
  }

  private fun createSentence(sentenceTypeUuid: UUID, uuid: UUID = sentenceUuid): SentenceEntity {
    val sentenceType = createSentenceType(sentenceTypeUuid, "Type")
    val charge = mockk<ChargeEntity>()
    every { charge.id } returns 1

    return mockk<SentenceEntity>(relaxed = true).also { sentenceEntity ->
      every { sentenceEntity.sentenceUuid } returns uuid
      every { sentenceEntity.sentenceType } returns sentenceType
      every { sentenceEntity.statusId } returns EntityStatus.ACTIVE
      every { sentenceEntity.charge } returns charge
      every { sentenceEntity.id } returns 1
    }
  }

  private fun createSentenceType(uuid: UUID, description: String): SentenceTypeEntity = mockk<SentenceTypeEntity>().also { typeEntity ->
    every { typeEntity.sentenceTypeUuid } returns uuid
    every { typeEntity.description } returns description
    every { typeEntity.status } returns ReferenceEntityStatus.ACTIVE
    every { typeEntity.classification } returns SentenceTypeClassification.STANDARD
    every { typeEntity.id } returns 1
  }
}
