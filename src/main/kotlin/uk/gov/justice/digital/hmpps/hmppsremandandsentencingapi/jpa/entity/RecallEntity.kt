package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateRecall
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.RecallType
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
  @Enumerated(EnumType.STRING)
  val recallType: RecallType,
  val createdAt: ZonedDateTime = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS),
  val createdByUsername: String,
  val createdByPrison: String,
) {

  fun copy(revocationDate: LocalDate, returnToCustodyDate: LocalDate, recallType: RecallType): RecallEntity {
    return RecallEntity(this.id, this.recallUuid, this.prisonerId, revocationDate, returnToCustodyDate, recallType, this.createdAt, this.createdByUsername, this.createdByPrison)
  }

  companion object {
    fun placeholderEntity(createRecall: CreateRecall, recallUuid: UUID? = null): RecallEntity =
      RecallEntity(
        recallUuid = recallUuid ?: UUID.randomUUID(),
        prisonerId = createRecall.prisonerId,
        revocationDate = createRecall.revocationDate,
        returnToCustodyDate = createRecall.returnToCustodyDate,
        recallType = createRecall.recallType,
        createdByUsername = createRecall.createdByUsername,
        createdByPrison = createRecall.createdByPrison,
      )
  }
}
