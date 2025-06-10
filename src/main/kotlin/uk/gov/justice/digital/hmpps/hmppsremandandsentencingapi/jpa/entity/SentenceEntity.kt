package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.annotations.Formula
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateSentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCreateSentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateSentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.SentenceLegacyData
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

@Entity
@Table(name = "sentence")
class SentenceEntity(
  @Id
  @Column
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Int = 0,
  @Column
  var sentenceUuid: UUID,
  @Column
  var chargeNumber: String? = null,
  @Column
  @Enumerated(EnumType.ORDINAL)
  var statusId: EntityStatus,
  @Column
  val createdAt: ZonedDateTime = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS),
  @Column
  val createdBy: String,
  @Column
  val createdPrison: String? = null,
  var updatedAt: ZonedDateTime? = null,
  var updatedBy: String? = null,
  var updatedPrison: String? = null,
  @Column
  var sentenceServeType: String,
  @OneToOne
  @JoinColumn(name = "consecutive_to_id")
  var consecutiveTo: SentenceEntity?,
  @OneToOne
  @JoinColumn(name = "sentence_type_id")
  var sentenceType: SentenceTypeEntity?,
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "superseding_sentence_id")
  var supersedingSentence: SentenceEntity?,
  @ManyToOne
  @JoinColumn(name = "charge_id")
  var charge: ChargeEntity,
  @Column
  var convictionDate: LocalDate?,
  @JdbcTypeCode(SqlTypes.JSON)
  var legacyData: SentenceLegacyData? = null,
  var fineAmount: BigDecimal?,
  @Formula("(select count(*) from recall_sentence rs where rs.sentence_id= id)")
  val totalRecallSentences: Int = -1,
) {
  @OneToMany
  @JoinColumn(name = "sentence_id")
  var periodLengths: MutableSet<PeriodLengthEntity> = mutableSetOf()

  @OneToMany(mappedBy = "sentence")
  var recallSentences: MutableSet<RecallSentenceEntity> = mutableSetOf()

  fun isSame(other: SentenceEntity?): Boolean = chargeNumber == other?.chargeNumber &&
    sentenceServeType == other?.sentenceServeType &&
    sentenceType == other.sentenceType &&
    createdPrison == other.createdPrison &&
    ((consecutiveTo == null && other.consecutiveTo == null) || consecutiveTo?.isSame(other.consecutiveTo) == true) &&
    convictionDate == other.convictionDate &&
    ((fineAmount == null && other.fineAmount == null) || (fineAmount != null && other.fineAmount?.compareTo(fineAmount) == 0)) &&
    statusId == other.statusId

  fun latestRecall(): RecallEntity? = recallSentences.map { it.recall }.filter { it.statusId == EntityStatus.ACTIVE }.maxByOrNull { it.createdAt }

  fun copyFrom(sentence: CreateSentence, createdBy: String, chargeEntity: ChargeEntity, consecutiveTo: SentenceEntity?, sentenceType: SentenceTypeEntity): SentenceEntity {
    val sentenceEntity = SentenceEntity(
      sentenceUuid = UUID.randomUUID(),
      chargeNumber = sentence.chargeNumber,
      statusId = EntityStatus.ACTIVE,
      createdBy = createdBy,
      createdPrison = sentence.prisonId,
      supersedingSentence = this,
      charge = chargeEntity,
      sentenceServeType = sentence.sentenceServeType,
      consecutiveTo = consecutiveTo,
      sentenceType = sentenceType,
      convictionDate = sentence.convictionDate,
      updatedAt = ZonedDateTime.now(),
      updatedBy = createdBy,
      updatedPrison = sentence.prisonId,
      fineAmount = sentence.fineAmount?.fineAmount,
    )
    sentenceEntity.periodLengths = sentence.periodLengths.map { PeriodLengthEntity.from(it, createdBy) }.toMutableSet()
    return sentenceEntity
  }

  fun copyFrom(sentence: LegacyCreateSentence, createdBy: String, consecutiveTo: SentenceEntity?, isManyCharges: Boolean): SentenceEntity {
    val sentenceEntity = SentenceEntity(
      sentenceUuid = UUID.randomUUID(),
      statusId = if (isManyCharges) {
        EntityStatus.MANY_CHARGES_DATA_FIX
      } else if (sentence.active) {
        EntityStatus.ACTIVE
      } else {
        EntityStatus.INACTIVE
      },
      createdBy = createdBy,
      supersedingSentence = this,
      charge = charge,
      sentenceServeType = if (consecutiveTo != null) "CONSECUTIVE" else "CONCURRENT",
      consecutiveTo = consecutiveTo,
      sentenceType = sentenceType,
      convictionDate = convictionDate,
      legacyData = sentence.legacyData,
      updatedAt = ZonedDateTime.now(),
      updatedBy = createdBy,
      fineAmount = sentence.fine?.fineAmount,
    )
    return sentenceEntity
  }

  fun copyFrom(sentence: MigrationCreateSentence, createdBy: String, chargeEntity: ChargeEntity, sentenceTypeEntity: SentenceTypeEntity?): SentenceEntity {
    sentence.legacyData.active = sentence.active
    return SentenceEntity(
      sentenceUuid = sentenceUuid,
      statusId = EntityStatus.MANY_CHARGES_DATA_FIX,
      createdBy = createdBy,
      createdPrison = null,
      supersedingSentence = null,
      charge = chargeEntity,
      sentenceServeType = if (sentence.consecutiveToSentenceId != null) "CONSECUTIVE" else "CONCURRENT",
      consecutiveTo = null,
      sentenceType = sentenceTypeEntity,
      convictionDate = null,
      legacyData = sentence.legacyData,
      fineAmount = sentence.fine?.fineAmount,
    )
  }

  fun updateFrom(sentence: SentenceEntity) {
    chargeNumber = sentence.chargeNumber
    statusId = sentence.statusId
    updatedAt = sentence.updatedAt
    updatedBy = sentence.updatedBy
    updatedPrison = sentence.updatedPrison
    sentenceServeType = sentence.sentenceServeType
    consecutiveTo = sentence.consecutiveTo
    sentenceType = sentence.sentenceType
    supersedingSentence = sentence.supersedingSentence
    charge = sentence.charge
    convictionDate = sentence.convictionDate
    legacyData = sentence.legacyData
    fineAmount = sentence.fineAmount
  }

  fun delete(updatedUser: String) {
    updatedAt = ZonedDateTime.now()
    updatedBy = updatedUser
    statusId = EntityStatus.DELETED
  }

  companion object {
    fun from(sentence: CreateSentence, createdBy: String, chargeEntity: ChargeEntity, consecutiveTo: SentenceEntity?, sentenceType: SentenceTypeEntity): SentenceEntity {
      val sentenceEntity = SentenceEntity(
        sentenceUuid = sentence.sentenceUuid ?: UUID.randomUUID(),
        chargeNumber = sentence.chargeNumber,
        statusId = EntityStatus.ACTIVE,
        createdBy = createdBy,
        createdPrison = sentence.prisonId,
        supersedingSentence = null,
        charge = chargeEntity,
        sentenceServeType = sentence.sentenceServeType,
        consecutiveTo = consecutiveTo,
        sentenceType = sentenceType,
        convictionDate = sentence.convictionDate,
        fineAmount = sentence.fineAmount?.fineAmount,
      )
      return sentenceEntity
    }

    fun from(
      sentence: LegacyCreateSentence,
      createdBy: String,
      chargeEntity: ChargeEntity,
      sentenceTypeEntity: SentenceTypeEntity?,
      consecutiveTo: SentenceEntity?,
      sentenceUuid: UUID,
      isManyCharges: Boolean,
      convictionDate: LocalDate? = null,
    ): SentenceEntity = SentenceEntity(
      sentenceUuid = sentenceUuid,
      statusId = if (isManyCharges) {
        EntityStatus.MANY_CHARGES_DATA_FIX
      } else if (sentence.active) {
        EntityStatus.ACTIVE
      } else {
        EntityStatus.INACTIVE
      },
      createdBy = createdBy,
      supersedingSentence = null,
      charge = chargeEntity,
      sentenceServeType = if (consecutiveTo != null) "CONSECUTIVE" else "CONCURRENT",
      consecutiveTo = consecutiveTo,
      sentenceType = sentenceTypeEntity,
      convictionDate = convictionDate,
      legacyData = sentence.legacyData,
      fineAmount = sentence.fine?.fineAmount,
    )

    fun from(sentence: MigrationCreateSentence, createdBy: String, chargeEntity: ChargeEntity, sentenceTypeEntity: SentenceTypeEntity?): SentenceEntity {
      sentence.legacyData.active = sentence.active
      return SentenceEntity(
        sentenceUuid = UUID.randomUUID(),
        statusId = if (sentence.active) EntityStatus.ACTIVE else EntityStatus.INACTIVE,
        createdBy = createdBy,
        createdPrison = null,
        supersedingSentence = null,
        charge = chargeEntity,
        sentenceServeType = if (sentence.consecutiveToSentenceId != null) "CONSECUTIVE" else "CONCURRENT",
        consecutiveTo = null,
        sentenceType = sentenceTypeEntity,
        convictionDate = null,
        legacyData = sentence.legacyData,
        fineAmount = sentence.fine?.fineAmount,
      )
    }
  }
}
