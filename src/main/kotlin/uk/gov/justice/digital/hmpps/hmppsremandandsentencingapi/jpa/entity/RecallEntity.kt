package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateRecall
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

@Entity
@Table(name = "recall")
class RecallEntity(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Int = 0,
  val recallUuid: UUID = UUID.randomUUID(),
  val prisonerId: String,
  val revocationDate: LocalDate,
  val returnToCustodyDate: LocalDate,
  @ManyToOne
  @JoinColumn(name = "recall_type_id")
  val recallType: RecallTypeEntity,
  val createdAt: ZonedDateTime = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS),
  val createdByUsername: String,
  val createdByPrison: String,
) {

  fun copy(revocationDate: LocalDate, returnToCustodyDate: LocalDate, recallType: RecallTypeEntity): RecallEntity {
    return RecallEntity(this.id, this.recallUuid, this.prisonerId, revocationDate, returnToCustodyDate, recallType, this.createdAt, this.createdByUsername, this.createdByPrison)
  }

  companion object {
    fun placeholderEntity(createRecall: CreateRecall, recallType: RecallTypeEntity, recallUuid: UUID? = null): RecallEntity =
      RecallEntity(
        recallUuid = recallUuid ?: UUID.randomUUID(),
        prisonerId = createRecall.prisonerId,
        revocationDate = createRecall.revocationDate,
        returnToCustodyDate = createRecall.returnToCustodyDate,
        recallType = recallType,
        createdByUsername = createRecall.createdByUsername,
        createdByPrison = createRecall.createdByPrison,
      )
  }
}
