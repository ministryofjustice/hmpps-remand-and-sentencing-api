package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.RecallSentenceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ChangeSource
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.RecallSentenceLegacyData
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.*

@Entity
@Table(name = "recall_sentence_history")
class RecallSentenceHistoryEntity(
  @Id
  @Column
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Int,
  val originalRecallSentenceId: Int,
  @Column
  val recallSentenceUuid: UUID,
  @ManyToOne
  @JoinColumn(name = "sentence_id")
  val sentence: SentenceEntity,
  @ManyToOne
  @JoinColumn(name = "recall_history_id")
  val recallHistory: RecallHistoryEntity,
  @JdbcTypeCode(SqlTypes.JSON)
  val legacyData: RecallSentenceLegacyData?,
  // Audit and status columns
  val createdAt: ZonedDateTime,
  val createdByUsername: String,
  val createdPrison: String?,
  val historyCreatedAt: ZonedDateTime,
  @Enumerated(EnumType.STRING)
  val changeSource: ChangeSource,
) {

  companion object {
    fun from(recall: RecallHistoryEntity, sentence: RecallSentenceEntity, changeSource: ChangeSource): RecallSentenceHistoryEntity = RecallSentenceHistoryEntity(
      id = 0,
      originalRecallSentenceId = sentence.id,
      recallSentenceUuid = sentence.recallSentenceUuid,
      sentence = sentence.sentence,
      recallHistory = recall,
      legacyData = sentence.legacyData,
      createdAt = sentence.createdAt,
      createdByUsername = sentence.createdByUsername,
      createdPrison = sentence.createdPrison,
      historyCreatedAt = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS),
      changeSource = changeSource,
    )
  }
}
