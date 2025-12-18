package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.EventSource
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.RecallEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.RecallSentenceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.RecallTypeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ChangeSource
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ChargeEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.RecallEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.RecallType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.RecallSentenceLegacyData
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*

class RecallSentenceHistoryEntityTest {

  @Test
  fun `can create history entity from original`() {
    val recallUuid = UUID.randomUUID()
    val recallSentenceUuid = UUID.randomUUID()
    val originalRecall = RecallEntity(
      id = 987654,
      recallUuid = recallUuid,
      prisonerId = "A1234BC",
      revocationDate = LocalDate.of(2021, 1, 1),
      returnToCustodyDate = LocalDate.of(2022, 2, 2),
      inPrisonOnRevocationDate = true,
      recallType = RecallTypeEntity(0, RecallType.LR, "LR"),
      statusId = RecallEntityStatus.ACTIVE,
      createdAt = ZonedDateTime.of(2023, 3, 3, 3, 3, 3, 3, ZoneId.systemDefault()),
      createdByUsername = "CREATOR",
      createdPrison = "FOO",
      updatedAt = ZonedDateTime.of(2024, 4, 4, 4, 4, 4, 4, ZoneId.systemDefault()),
      updatedBy = "UPDATER",
      updatedPrison = "BAR",
      source = EventSource.DPS,
    )
    val sentence = SentenceEntity(
      sentenceUuid = UUID.randomUUID(),
      statusId = SentenceEntityStatus.ACTIVE,
      createdBy = "USER",
      sentenceServeType = "CONCURRENT",
      consecutiveTo = null,
      supersedingSentence = null,
      charge = ChargeEntity(
        chargeUuid = UUID.randomUUID(),
        offenceCode = "TEST123",
        statusId = ChargeEntityStatus.ACTIVE,
        createdBy = "test-user",
        offenceStartDate = null,
        offenceEndDate = null,
        chargeOutcome = null,
        supersedingCharge = null,
        terrorRelated = null,
        foreignPowerRelated = null,
        domesticViolenceRelated = null,
        createdPrison = null,
        legacyData = null,
        appearanceCharges = mutableSetOf(),
      ),
      convictionDate = null,
      legacyData = DataCreator.sentenceLegacyData(),
      fineAmount = null,
      countNumber = null,
      sentenceType = null,
    )

    val original = RecallSentenceEntity(
      id = 999888,
      recallSentenceUuid = recallSentenceUuid,
      sentence = sentence,
      recall = originalRecall,
      legacyData = RecallSentenceLegacyData(null, null, null, "2025-05-05", null),
      createdAt = ZonedDateTime.of(2025, 5, 5, 5, 5, 5, 5, ZoneId.systemDefault()),
      createdByUsername = "CREATOR",
      createdPrison = "BAR",
    )

    val historyRecall = RecallHistoryEntity.from(originalRecall, RecallEntityStatus.EDITED, ChangeSource.DPS)
    assertThat(RecallSentenceHistoryEntity.from(historyRecall, original, ChangeSource.DPS))
      .usingRecursiveComparison()
      .ignoringFields("historyCreatedAt")
      .isEqualTo(
        RecallSentenceHistoryEntity(
          id = 0,
          originalRecallSentenceId = 999888,
          recallSentenceUuid = recallSentenceUuid,
          sentence = sentence,
          recallHistory = historyRecall,
          legacyData = RecallSentenceLegacyData(null, null, null, "2025-05-05", null),
          createdAt = ZonedDateTime.of(2025, 5, 5, 5, 5, 5, 5, ZoneId.systemDefault()),
          createdByUsername = "CREATOR",
          createdPrison = "BAR",
          historyCreatedAt = ZonedDateTime.now(), // ignored in assertions
          preRecallSentenceStatus = null,
          changeSource = ChangeSource.DPS,
        ),
      )
  }
}
