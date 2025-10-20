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
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateRecall
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.EventSource
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.EventSource.DPS
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.EventSource.NOMIS
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.RecallEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCreateSentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateSentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.booking.BookingCreateSentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.merge.MergeCreateSentence
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
  var prisonerId: String,
  var revocationDate: LocalDate?,
  var returnToCustodyDate: LocalDate?,
  var inPrisonOnRevocationDate: Boolean?,
  @ManyToOne
  @JoinColumn(name = "recall_type_id")
  var recallType: RecallTypeEntity,

  // Audit and status columns
  @Column
  @Enumerated(EnumType.ORDINAL)
  var statusId: RecallEntityStatus,
  val createdAt: ZonedDateTime = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS),
  val createdByUsername: String,
  val createdPrison: String? = null,
  var updatedAt: ZonedDateTime? = null,
  var updatedBy: String? = null,
  var updatedPrison: String? = null,
  @Enumerated(EnumType.STRING)
  var source: EventSource = DPS,
) {

  @OneToMany(mappedBy = "recall")
  var recallSentences: MutableSet<RecallSentenceEntity> = mutableSetOf()

  fun delete(updatedUser: String) {
    updatedAt = ZonedDateTime.now()
    updatedBy = updatedUser
    statusId = RecallEntityStatus.DELETED
  }

  companion object {
    fun fromDps(createRecall: CreateRecall, recallType: RecallTypeEntity, recallUuid: UUID? = null): RecallEntity = RecallEntity(
      recallUuid = recallUuid ?: UUID.randomUUID(),
      prisonerId = createRecall.prisonerId,
      revocationDate = createRecall.revocationDate,
      returnToCustodyDate = createRecall.returnToCustodyDate,
      inPrisonOnRevocationDate = createRecall.inPrisonOnRevocationDate,
      recallType = recallType,
      createdByUsername = createRecall.createdByUsername,
      createdPrison = createRecall.createdByPrison,
      statusId = RecallEntityStatus.ACTIVE,
      source = DPS,
    )

    fun fromMigration(
      sentence: MigrationCreateSentence,
      prisonerId: String,
      createdByUsername: String,
      recallType: RecallTypeEntity,
    ): RecallEntity = RecallEntity(
      prisonerId = prisonerId,
      revocationDate = null,
      returnToCustodyDate = if (recallType.code.isFixedTermRecall()) sentence.returnToCustodyDate else null,
      inPrisonOnRevocationDate = null,
      recallType = recallType,
      createdByUsername = createdByUsername,
      statusId = RecallEntityStatus.ACTIVE,
      source = NOMIS,
    )

    fun fromMerge(
      sentence: MergeCreateSentence,
      prisonerId: String,
      createdByUsername: String,
      recallType: RecallTypeEntity,
    ): RecallEntity = RecallEntity(
      prisonerId = prisonerId,
      revocationDate = null,
      returnToCustodyDate = if (recallType.code.isFixedTermRecall()) sentence.returnToCustodyDate else null,
      inPrisonOnRevocationDate = null,
      recallType = recallType,
      createdByUsername = createdByUsername,
      statusId = RecallEntityStatus.ACTIVE,
      source = NOMIS,
    )

    fun fromBooking(
      sentence: BookingCreateSentence,
      prisonerId: String,
      createdByUsername: String,
      recallType: RecallTypeEntity,
    ): RecallEntity = RecallEntity(
      prisonerId = prisonerId,
      revocationDate = null,
      returnToCustodyDate = if (recallType.code.isFixedTermRecall()) sentence.returnToCustodyDate else null,
      inPrisonOnRevocationDate = null,
      recallType = recallType,
      createdByUsername = createdByUsername,
      statusId = RecallEntityStatus.DUPLICATE,
      source = NOMIS,
    )

    fun fromLegacy(
      sentence: LegacyCreateSentence,
      prisonerId: String,
      createdByUsername: String,
      recallType: RecallTypeEntity,
    ): RecallEntity = RecallEntity(
      prisonerId = prisonerId,
      revocationDate = null,
      returnToCustodyDate = if (recallType.code.isFixedTermRecall()) sentence.returnToCustodyDate else null,
      inPrisonOnRevocationDate = null,
      recallType = recallType,
      createdByUsername = createdByUsername,
      statusId = RecallEntityStatus.ACTIVE,
      source = NOMIS,
    )
  }
}
