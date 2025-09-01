package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCreateSentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.RecallSentenceLegacyData
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

@Entity
@Table(name = "recall_sentence")
class RecallSentenceEntity(
  @Id
  @Column
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Int = 0,
  @Column
  var recallSentenceUuid: UUID,
  @ManyToOne
  @JoinColumn(name = "sentence_id")
  var sentence: SentenceEntity,
  @ManyToOne
  @JoinColumn(name = "recall_id")
  var recall: RecallEntity,
  @JdbcTypeCode(SqlTypes.JSON)
  var legacyData: RecallSentenceLegacyData? = null,
  // Audit and status columns
  val createdAt: ZonedDateTime = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS),
  val createdByUsername: String,
  val createdPrison: String? = null,
) {

  companion object {
    fun placeholderEntity(recall: RecallEntity, sentence: SentenceEntity): RecallSentenceEntity = RecallSentenceEntity(
      recallSentenceUuid = UUID.randomUUID(),
      sentence = sentence,
      recall = recall,
      createdByUsername = recall.createdByUsername,
      createdPrison = recall.createdPrison,
    )

    fun fromMigration(createdSentence: SentenceEntity, recall: RecallEntity, createdByUsername: String, legacyData: RecallSentenceLegacyData) = RecallSentenceEntity(
      recallSentenceUuid = UUID.randomUUID(),
      sentence = createdSentence,
      recall = recall,
      createdByUsername = createdByUsername,
      legacyData = legacyData,
    )

    fun fromMerge(createdSentence: SentenceEntity, recall: RecallEntity, createdByUsername: String, legacyData: RecallSentenceLegacyData) = RecallSentenceEntity(
      recallSentenceUuid = UUID.randomUUID(),
      sentence = createdSentence,
      recall = recall,
      createdByUsername = createdByUsername,
      legacyData = legacyData,
    )

    fun fromBooking(createdSentence: SentenceEntity, recall: RecallEntity, createdByUsername: String, legacyData: RecallSentenceLegacyData) = RecallSentenceEntity(
      recallSentenceUuid = UUID.randomUUID(),
      sentence = createdSentence,
      recall = recall,
      createdByUsername = createdByUsername,
      legacyData = legacyData,
    )

    fun from(sentence: LegacyCreateSentence, createdSentence: SentenceEntity, recall: RecallEntity, createdByUsername: String, legacyData: RecallSentenceLegacyData) = RecallSentenceEntity(
      recallSentenceUuid = UUID.randomUUID(),
      sentence = createdSentence,
      recall = recall,
      createdByUsername = createdByUsername,
      legacyData = legacyData,
    )
  }
}
