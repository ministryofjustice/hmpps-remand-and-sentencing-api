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
  val recallUniqueIdentifier: UUID = UUID.randomUUID(),
  val prisonerId: String,
  val recallDate: LocalDate,
  val returnToCustodyDate: LocalDate,
  @Enumerated(EnumType.STRING)
  val recallType: RecallType,
  val createdAt: ZonedDateTime = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS),
  val createdByUsername: String,
) {

  fun copy(recallDate: LocalDate, returnToCustodyDate: LocalDate, recallType: RecallType): RecallEntity {
    return RecallEntity(this.id, this.recallUniqueIdentifier, this.prisonerId, recallDate, returnToCustodyDate, recallType, this.createdAt, this.createdByUsername)
  }

  companion object {
    fun placeholderEntity(createRecall: CreateRecall, recallUniqueIdentifier: UUID? = null): RecallEntity =
      RecallEntity(
        recallUniqueIdentifier = recallUniqueIdentifier ?: UUID.randomUUID(),
        prisonerId = createRecall.prisonerId,
        recallDate = createRecall.recallDate,
        returnToCustodyDate = createRecall.returnToCustodyDate,
        recallType = createRecall.recallType,
        createdByUsername = createRecall.createdByUsername,
      )
  }
}
