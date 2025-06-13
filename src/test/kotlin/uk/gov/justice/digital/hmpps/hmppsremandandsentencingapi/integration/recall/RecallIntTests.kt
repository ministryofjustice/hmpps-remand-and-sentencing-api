package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.recall

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateRecall
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.Recall
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.RecallType.FTR_14
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.RecallType.FTR_28
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.RecallType.LR
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

class RecallIntTests : IntegrationTestBase() {

  @Test
  fun `Create recall and fetch it based on returned UUID`() {
    val recall = CreateRecall(
      prisonerId = "A12345B",
      revocationDate = LocalDate.of(2024, 1, 2),
      returnToCustodyDate = LocalDate.of(2024, 2, 3),
      recallTypeCode = FTR_14,
      createdByUsername = "user001",
      createdByPrison = "PRI",
    )

    val createRecall = createRecall(recall)
    val actualRecall = getRecallByUUID(createRecall.recallUuid)

    assertThat(actualRecall)
      .usingRecursiveComparison()
      .ignoringFields("createdAt")
      .isEqualTo(
        Recall(
          recallUuid = createRecall.recallUuid,
          prisonerId = "A12345B",
          revocationDate = LocalDate.of(2024, 1, 2),
          returnToCustodyDate = LocalDate.of(2024, 2, 3),
          recallType = FTR_14,
          createdByUsername = "user001",
          createdAt = ZonedDateTime.now(),
          createdByPrison = "PRI",
          sentences = emptyList(),
        ),
      )
    val messages = getMessages(1)
    assertThat(messages).hasSize(1).extracting<String> { it.eventType }.contains("recall.inserted")
  }

  @Test
  fun `Create recall with no dates and fetch it based on returned UUID`() {
    val recall = CreateRecall(
      prisonerId = "A12345B",
      revocationDate = null,
      returnToCustodyDate = null,
      recallTypeCode = FTR_14,
      createdByUsername = "user001",
      createdByPrison = "PRI",
    )

    val createRecall = createRecall(recall)
    val actualRecall = getRecallByUUID(createRecall.recallUuid)

    assertThat(actualRecall)
      .usingRecursiveComparison()
      .ignoringFields("createdAt")
      .isEqualTo(
        Recall(
          recallUuid = createRecall.recallUuid,
          prisonerId = "A12345B",
          revocationDate = null,
          returnToCustodyDate = null,
          recallType = FTR_14,
          createdByUsername = "user001",
          createdAt = ZonedDateTime.now(),
          createdByPrison = "PRI",
          sentences = emptyList(),
        ),
      )
    val messages = getMessages(1)
    assertThat(messages).hasSize(1).extracting<String> { it.eventType }.contains("recall.inserted")
  }

  @Test
  fun `Create recall with a uuid via edit endpoint`() {
    val recall = CreateRecall(
      prisonerId = "A12345B",
      revocationDate = LocalDate.of(2024, 1, 2),
      returnToCustodyDate = LocalDate.of(2024, 2, 3),
      recallTypeCode = FTR_28,
      createdByUsername = "user001",
      createdByPrison = "PRI",
    )

    val uuid = UUID.randomUUID()

    val createdRecall = updateRecall(recall, uuid)

    assertThat(uuid).isEqualTo(createdRecall.recallUuid)
    val actualRecall = getRecallByUUID(createdRecall.recallUuid)

    assertThat(actualRecall)
      .usingRecursiveComparison()
      .ignoringFields("createdAt")
      .isEqualTo(
        Recall(
          recallUuid = createdRecall.recallUuid,
          prisonerId = "A12345B",
          revocationDate = LocalDate.of(2024, 1, 2),
          returnToCustodyDate = LocalDate.of(2024, 2, 3),
          recallType = FTR_28,
          createdByUsername = "user001",
          createdAt = ZonedDateTime.now(),
          createdByPrison = "PRI",
          sentences = emptyList(),
        ),
      )
    val messages = getMessages(1)
    assertThat(messages).hasSize(1).extracting<String> { it.eventType }.contains("recall.inserted")
  }

  @Test
  fun `Get all recalls for a prisoner`() {
    val (sentenceOne, _) = createCourtCaseTwoSentences()
    val recallOne = DpsDataCreator.dpsCreateRecall(
      revocationDate = LocalDate.of(2024, 7, 1),
      returnToCustodyDate = LocalDate.of(2024, 7, 1),
      recallTypeCode = LR,
      sentenceIds = listOf(
        sentenceOne.sentenceUuid!!,
      ),
    )
    val uuidOne = createRecall(recallOne).recallUuid
    val recallTwo = DpsDataCreator.dpsCreateRecall(
      revocationDate = LocalDate.of(2024, 9, 1),
      returnToCustodyDate = LocalDate.of(2024, 9, 1),
      recallTypeCode = FTR_14,
      sentenceIds = listOf(
        sentenceOne.sentenceUuid!!,
      ),
    )
    val uuidTwo = createRecall(recallTwo).recallUuid

    val recalls = getRecallsByPrisonerId(DpsDataCreator.DEFAULT_PRISONER_ID)

    assertThat(recalls)
      .usingRecursiveComparison()
      .ignoringFields("createdAt", "sentences", "courtCaseIds")
      .ignoringCollectionOrder()
      .isEqualTo(
        listOf(
          Recall(
            recallUuid = uuidOne,
            prisonerId = DpsDataCreator.DEFAULT_PRISONER_ID,
            revocationDate = LocalDate.of(2024, 7, 1),
            returnToCustodyDate = LocalDate.of(2024, 7, 1),
            recallType = LR,
            createdByUsername = "user001",
            createdAt = ZonedDateTime.now(),
            createdByPrison = "PRISON1",
          ),
          Recall(
            recallUuid = uuidTwo,
            prisonerId = DpsDataCreator.DEFAULT_PRISONER_ID,
            revocationDate = LocalDate.of(2024, 9, 1),
            returnToCustodyDate = LocalDate.of(2024, 9, 1),
            recallType = FTR_14,
            createdByUsername = "user001",
            createdAt = ZonedDateTime.now(),
            createdByPrison = "PRISON1",
          ),
        ),
      )
  }

  @Test
  fun `Update a recall`() {
    val (sentenceOne, sentenceTwo) = createCourtCaseTwoSentences()
    val originalRecall = DpsDataCreator.dpsCreateRecall(
      sentenceIds = listOf(
        sentenceOne.sentenceUuid!!,
        sentenceTwo.sentenceUuid!!,
      ),
    )
    val uuid = createRecall(originalRecall).recallUuid
    purgeQueues()

    updateRecall(
      CreateRecall(
        prisonerId = "A12345B",
        recallTypeCode = FTR_14,
        revocationDate = originalRecall.revocationDate,
        returnToCustodyDate = originalRecall.returnToCustodyDate,
        createdByUsername = "user001",
        createdByPrison = "New prison",
        sentenceIds = listOf(sentenceOne.sentenceUuid!!),
      ),
      uuid,
    )

    val savedRecall = getRecallByUUID(uuid)

    assertThat(savedRecall)
      .usingRecursiveComparison()
      .ignoringFields("createdAt", "sentences", "courtCaseIds")
      .ignoringCollectionOrder()
      .isEqualTo(
        Recall(
          recallUuid = uuid,
          prisonerId = originalRecall.prisonerId,
          revocationDate = originalRecall.revocationDate,
          returnToCustodyDate = originalRecall.returnToCustodyDate,
          recallType = FTR_14,
          createdByUsername = originalRecall.createdByUsername,
          createdByPrison = originalRecall.createdByPrison,
          createdAt = ZonedDateTime.now(),
        ),
      )

    assertThat(savedRecall.sentences).hasSize(1)
    assertThat(savedRecall.sentences).extracting<UUID> { it.sentenceUuid }.contains(sentenceOne.sentenceUuid)

    val messages = getMessages(1)
    assertThat(messages).hasSize(1).extracting<String> { it.eventType }.contains("recall.updated")
    val message = messages[0]
    val sentenceIds = message.additionalInformation.get("sentenceIds").toList().map { arr -> arr.asText() }
    val previousSentenceIds = message.additionalInformation.get("previousSentenceIds").toList().map { arr -> arr.asText() }
    assertThat(sentenceIds)
      .contains(sentenceOne.sentenceUuid.toString())
    assertThat(previousSentenceIds)
      .contains(sentenceOne.sentenceUuid.toString())
      .contains(sentenceTwo.sentenceUuid.toString())
  }

  @Test
  fun `Create recall with a sentence and fetch it based on returned UUID`() {
    val (sentenceOne, _) = createCourtCaseTwoSentences()
    val recall = DpsDataCreator.dpsCreateRecall(
      sentenceIds = listOf(
        sentenceOne.sentenceUuid!!,
      ),
    )

    val createRecall = createRecall(recall)
    val actualRecall = getRecallByUUID(createRecall.recallUuid)
    assertThat(actualRecall)
      .usingRecursiveComparison()
      .ignoringFields("createdAt", "sentences", "courtCaseIds")
      .isEqualTo(
        Recall(
          recallUuid = createRecall.recallUuid,
          prisonerId = DpsDataCreator.DEFAULT_PRISONER_ID,
          revocationDate = LocalDate.of(2024, 1, 2),
          returnToCustodyDate = LocalDate.of(2024, 2, 3),
          recallType = FTR_14,
          createdByUsername = "user001",
          createdAt = ZonedDateTime.now(),
          createdByPrison = "PRISON1",
        ),
      )
    val messages = getMessages(1)
    assertThat(messages).hasSize(1).extracting<String> { it.eventType }.contains("recall.inserted")
  }

  @Test
  fun `Create recall with two sentences and fetch it based on returned UUID`() {
    val (sentenceOne, sentenceTwo) = createCourtCaseTwoSentences()
    val recall = DpsDataCreator.dpsCreateRecall(
      sentenceIds = listOf(
        sentenceOne.sentenceUuid!!,
        sentenceTwo.sentenceUuid!!,
      ),
    )

    val createRecall = createRecall(recall)
    val actualRecall = getRecallByUUID(createRecall.recallUuid)
    assertThat(actualRecall)
      .usingRecursiveComparison()
      .ignoringFields("createdAt", "sentences", "courtCaseIds")
      .isEqualTo(
        Recall(
          recallUuid = createRecall.recallUuid,
          prisonerId = DpsDataCreator.DEFAULT_PRISONER_ID,
          revocationDate = LocalDate.of(2024, 1, 2),
          returnToCustodyDate = LocalDate.of(2024, 2, 3),
          recallType = FTR_14,
          createdByUsername = "user001",
          createdAt = ZonedDateTime.now(),
          createdByPrison = "PRISON1",
        ),
      )
    val messages = getMessages(1)
    assertThat(messages).hasSize(1).extracting<String> { it.eventType }.contains("recall.inserted")
  }

  @Test
  fun `Fetch created recall by UUID ignoring unrelated recall_sentences`() {
    val recall = CreateRecall(
      prisonerId = "A12345B",
      revocationDate = LocalDate.of(2024, 1, 2),
      returnToCustodyDate = LocalDate.of(2024, 2, 3),
      recallTypeCode = FTR_14,
      createdByUsername = "user001",
      createdByPrison = "PRI",
      sentenceIds = emptyList(),
    )

    val createRecall = createRecall(recall)
    val actualRecall = getRecallByUUID(createRecall.recallUuid)

    assertThat(actualRecall)
      .usingRecursiveComparison()
      .ignoringFields("createdAt")
      .isEqualTo(
        Recall(
          recallUuid = createRecall.recallUuid,
          prisonerId = "A12345B",
          revocationDate = LocalDate.of(2024, 1, 2),
          returnToCustodyDate = LocalDate.of(2024, 2, 3),
          recallType = FTR_14,
          createdByUsername = "user001",
          createdAt = ZonedDateTime.now(),
          createdByPrison = "PRI",
          sentences = emptyList(),
        ),
      )
    val messages = getMessages(1)
    assertThat(messages).hasSize(1).extracting<String> { it.eventType }.contains("recall.inserted")
  }

  @Test
  fun `Delete a recall`() {
    val (sentenceOne, sentenceTwo) = createCourtCaseTwoSentences()
    val recall = DpsDataCreator.dpsCreateRecall(
      sentenceIds = listOf(
        sentenceOne.sentenceUuid!!,
        sentenceTwo.sentenceUuid!!,
      ),
    )
    val createRecall = createRecall(recall)
    purgeQueues()

    deleteRecall(createRecall.recallUuid)

    val recalls = getRecallsByPrisonerId(DpsDataCreator.DEFAULT_PRISONER_ID)

    assertThat(recalls).isEmpty()

    val messages = getMessages(1)
    assertThat(messages).hasSize(1).extracting<String> { it.eventType }.contains("recall.deleted")
  }

  @Test
  fun `Delete a recall where many recalls exist`() {
    val (sentenceOne, sentenceTwo) = createCourtCaseTwoSentences()
    val recallOne = DpsDataCreator.dpsCreateRecall(
      sentenceIds = listOf(
        sentenceOne.sentenceUuid!!,
        sentenceTwo.sentenceUuid!!,
      ),
    )
    val recallTwo = DpsDataCreator.dpsCreateRecall(
      sentenceIds = listOf(
        sentenceOne.sentenceUuid!!,
      ),
      revocationDate = recallOne.revocationDate!!.plusWeeks(4),
      returnToCustodyDate = recallOne.returnToCustodyDate!!.plusWeeks(4),
    )
    val recallOneId = createRecall(recallOne).recallUuid
    val recallTwoId = createRecall(recallTwo).recallUuid
    purgeQueues()

    deleteRecall(recallTwoId)

    val recalls = getRecallsByPrisonerId(DpsDataCreator.DEFAULT_PRISONER_ID)

    assertThat(recalls).hasSize(1)
    assertThat(recalls[0].recallUuid).isEqualTo(recallOneId)

    val messages = getMessages(1)
    assertThat(messages).hasSize(1)
      .extracting<String> { it.eventType }.contains("recall.deleted")
    assertThat(messages[0].additionalInformation.get("previousRecallId").asText()).isEqualTo(recallOneId.toString())
  }

  @Test
  fun `Delete a legacy recall should also delete sentence`() {
    // Create a legacy sentence so that the legacy recall is also created.
    val (lifetimeUuid, createdSentence) = createLegacySentence(
      legacySentence = DataCreator.legacyCreateSentence(sentenceLegacyData = DataCreator.sentenceLegacyData(sentenceCalcType = "FTR_ORA", sentenceCategory = "2020"), returnToCustodyDate = LocalDate.of(2023, 1, 1)),
    )
    val recall = getRecallsByPrisonerId(DpsDataCreator.DEFAULT_PRISONER_ID).first()
    purgeQueues()

    deleteRecall(recall.recallUuid)

    val recalls = getRecallsByPrisonerId(DpsDataCreator.DEFAULT_PRISONER_ID)

    assertThat(recalls).isEmpty()

    val messages = getMessages(2)
    assertThat(messages).hasSize(2)
      .extracting<String> { it.eventType }.contains("recall.deleted", "sentence.deleted")
  }
}
