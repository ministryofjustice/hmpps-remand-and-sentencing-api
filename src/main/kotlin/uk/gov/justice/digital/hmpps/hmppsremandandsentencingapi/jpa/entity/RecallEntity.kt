package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity

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
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateRecall
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCreateSentence
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
  var revocationDate: LocalDate?,
  var returnToCustodyDate: LocalDate?,
  @ManyToOne
  @JoinColumn(name = "recall_type_id")
  var recallType: RecallTypeEntity,

  // Audit and status columns
  @Column
  @Enumerated(EnumType.ORDINAL)
  var statusId: EntityStatus,
  val createdAt: ZonedDateTime = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS),
  val createdByUsername: String,
  val createdPrison: String? = null,
  var updatedAt: ZonedDateTime? = null,
  var updatedBy: String? = null,
  var updatedPrison: String? = null,
) {

  companion object {
    fun placeholderEntity(createRecall: CreateRecall, recallType: RecallTypeEntity, recallUuid: UUID? = null): RecallEntity = RecallEntity(
      recallUuid = recallUuid ?: UUID.randomUUID(),
      prisonerId = createRecall.prisonerId,
      revocationDate = createRecall.revocationDate,
      returnToCustodyDate = createRecall.returnToCustodyDate,
      recallType = recallType,
      createdByUsername = createRecall.createdByUsername,
      createdPrison = createRecall.createdByPrison,
      statusId = EntityStatus.ACTIVE,
    )

    fun fromMigration(prisonerId: String, createdByUsername: String, recallType: RecallTypeEntity): RecallEntity = RecallEntity(
      prisonerId = prisonerId,
      revocationDate = null,
      returnToCustodyDate = null, // TODO RCLL-371
      recallType = recallType,
      createdByUsername = createdByUsername,
      statusId = EntityStatus.ACTIVE,
    )
    fun from(sentence: LegacyCreateSentence, prisonerId: String, createdByUsername: String, recallType: RecallTypeEntity): RecallEntity = RecallEntity(
      prisonerId = prisonerId,
      revocationDate = null,
      returnToCustodyDate = null, // TODO RCLL-371
      recallType = recallType,
      createdByUsername = createdByUsername,
      createdPrison = sentence.prisonId,
      statusId = EntityStatus.ACTIVE,
    )
  }
}
