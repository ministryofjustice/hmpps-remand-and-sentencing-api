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
  @Column
  val createdAt: ZonedDateTime = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS),
  @Column
  val createdByUsername: String,
  @Column
  val createdByPrison: String?,
  @JdbcTypeCode(SqlTypes.JSON)
  var legacyData: RecallSentenceLegacyData? = null,
) {

  companion object {
    fun placeholderEntity(recall: RecallEntity, sentence: SentenceEntity): RecallSentenceEntity = RecallSentenceEntity(
      recallSentenceUuid = UUID.randomUUID(),
      sentence = sentence,
      recall = recall,
      createdByUsername = recall.createdByUsername,
      createdByPrison = recall.createdByPrison,
    )
  }
}
