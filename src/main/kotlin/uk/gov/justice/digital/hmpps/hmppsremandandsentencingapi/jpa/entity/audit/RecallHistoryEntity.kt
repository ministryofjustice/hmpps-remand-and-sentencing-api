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
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.RecallEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.RecallTypeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
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
  val recallId: Int,
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
  val statusId: EntityStatus,
  val createdAt: ZonedDateTime,
  val createdByUsername: String,
  val createdPrison: String?,
  val updatedAt: ZonedDateTime?,
  val updatedBy: String?,
  val updatedPrison: String?,
  @Column
  @Enumerated(EnumType.ORDINAL)
  val historyStatusId: EntityStatus,
  val historyCreatedAt: ZonedDateTime,
) {

  companion object {
    fun from(original: RecallEntity, historyStatus: EntityStatus) = RecallHistoryEntity(
      id = 0,
      recallId = original.id,
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
      historyStatusId = historyStatus,
      historyCreatedAt = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS),
    )
  }
}
