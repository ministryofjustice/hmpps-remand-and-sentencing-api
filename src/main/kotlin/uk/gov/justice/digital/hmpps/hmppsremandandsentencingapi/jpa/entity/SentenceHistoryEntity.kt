package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.SentenceLegacyData
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

@Entity
@Table(name = "sentence_history")
class SentenceHistoryEntity(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Int = 0,
  var sentenceUuid: UUID,
  val chargeNumber: String?,
  @Enumerated(EnumType.ORDINAL)
  var statusId: EntityStatus,
  val createdAt: ZonedDateTime,
  val createdBy: String,
  val createdPrison: String?,
  val updatedAt: ZonedDateTime?,
  val updatedBy: String?,
  val updatedPrison: String?,
  val sentenceServeType: String,
  val supersedingSentenceId: Int?,
  val chargeId: Int,
  val consecutiveToId: Int?,
  val convictionDate: LocalDate?,
  val sentenceTypeId: Int?,
  @JdbcTypeCode(SqlTypes.JSON)
  var legacyData: SentenceLegacyData?,
  @OneToOne
  @JoinColumn(name = "original_sentence_id")
  val originalSentence: SentenceEntity?,
)
