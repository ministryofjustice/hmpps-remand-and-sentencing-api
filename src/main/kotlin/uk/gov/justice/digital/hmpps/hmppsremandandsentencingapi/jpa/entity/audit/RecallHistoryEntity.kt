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
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.EventSource
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.EventSource.DPS
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.RecallEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.RecallTypeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.RecallEntityStatus
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.*

@Entity
@Table(name = "recall_history")
class RecallHistoryEntity(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Int = 0,
  val originalRecallId: Int,
  val recallUuid: UUID,
  val prisonerId: String,
  val revocationDate: LocalDate?,
  val returnToCustodyDate: LocalDate?,
  val inPrisonOnRevocationDate: Boolean?,
  @ManyToOne
  @JoinColumn(name = "recall_type_id")
  val recallType: RecallTypeEntity,

  // Audit and status columns
  @Column
  @Enumerated(EnumType.ORDINAL)
  val statusId: RecallEntityStatus,
  val createdAt: ZonedDateTime,
  val createdByUsername: String,
  val createdPrison: String?,
  val updatedAt: ZonedDateTime?,
  val updatedBy: String?,
  val updatedPrison: String?,
  @Enumerated(EnumType.STRING)
  var source: EventSource = DPS,
  @Column
  @Enumerated(EnumType.ORDINAL)
  val historyStatusId: RecallEntityStatus,
  val historyCreatedAt: ZonedDateTime,
) {

  companion object {
    fun from(original: RecallEntity, historyStatus: RecallEntityStatus) = RecallHistoryEntity(
      id = 0,
      originalRecallId = original.id,
      recallUuid = original.recallUuid,
      prisonerId = original.prisonerId,
      revocationDate = original.revocationDate,
      returnToCustodyDate = original.returnToCustodyDate,
      inPrisonOnRevocationDate = original.inPrisonOnRevocationDate,
      recallType = original.recallType,
      statusId = original.statusId,
      createdAt = original.createdAt,
      createdByUsername = original.createdByUsername,
      createdPrison = original.createdPrison,
      updatedAt = original.updatedAt,
      updatedBy = original.updatedBy,
      updatedPrison = original.updatedPrison,
      source = original.source,
      historyStatusId = historyStatus,
      historyCreatedAt = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS),
    )
  }
}
