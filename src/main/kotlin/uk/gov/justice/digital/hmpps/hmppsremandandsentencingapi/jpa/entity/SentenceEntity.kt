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
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateSentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCreateSentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateSentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.SentenceLegacyData
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
  var chargeNumber: String?,
  @Column
  @Enumerated(EnumType.ORDINAL)
  var statusId: EntityStatus,
  @Column
  val createdAt: ZonedDateTime = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS),
  @Column
  val createdBy: String,
  @Column
  val createdPrison: String?,
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

) {
  @OneToMany
  @JoinColumn(name = "sentence_id")
  var periodLengths: MutableList<PeriodLengthEntity> = mutableListOf()

  @OneToOne(mappedBy = "sentenceEntity")
  var fineAmountEntity: FineAmountEntity? = null

  fun isSame(other: SentenceEntity?): Boolean = chargeNumber == other?.chargeNumber &&
    periodLengths.all { periodLength -> other?.periodLengths?.any { otherPeriodLength -> periodLength.isSame(otherPeriodLength) } == true } &&
    sentenceServeType == other?.sentenceServeType &&
    sentenceType == other.sentenceType &&
    createdPrison == other.createdPrison &&
    ((consecutiveTo == null && other.consecutiveTo == null) || consecutiveTo?.isSame(other.consecutiveTo) == true) &&
    convictionDate == other.convictionDate &&
    ((fineAmountEntity == null && other.fineAmountEntity == null) || fineAmountEntity?.isSame(other.fineAmountEntity) == true)

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
    )
    sentenceEntity.periodLengths = sentence.periodLengths.map { PeriodLengthEntity.from(it) }.toMutableList()
    sentenceEntity.fineAmountEntity = sentence.fineAmount?.let { FineAmountEntity.from(it) }
    return sentenceEntity
  }

  fun copyFrom(sentence: LegacyCreateSentence, createdBy: String, sentenceTypeEntity: SentenceTypeEntity?, consecutiveTo: SentenceEntity?): SentenceEntity {
    val sentenceEntity = SentenceEntity(
      sentenceUuid = UUID.randomUUID(),
      chargeNumber = sentence.chargeNumber,
      statusId = if (sentence.active) EntityStatus.ACTIVE else EntityStatus.INACTIVE,
      createdBy = createdBy,
      createdPrison = sentence.prisonId,
      supersedingSentence = this,
      charge = charge,
      sentenceServeType = if (consecutiveTo != null) "CONSECUTIVE" else "UNKNOWN",
      consecutiveTo = consecutiveTo,
      sentenceType = sentenceTypeEntity,
      convictionDate = convictionDate,
      legacyData = sentence.legacyData,
      updatedAt = ZonedDateTime.now(),
      updatedBy = createdBy,
      updatedPrison = sentence.prisonId,
    )
    sentenceEntity.periodLengths = sentence.periodLengths.map { PeriodLengthEntity.from(it, sentenceTypeEntity?.nomisSentenceCalcType ?: sentence.legacyData.sentenceCalcType!!) }.toMutableList()
    sentenceEntity.fineAmountEntity = sentence.fine?.let { FineAmountEntity.from(it) }
    return sentenceEntity
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
      )
      return sentenceEntity
    }

    fun from(sentence: LegacyCreateSentence, createdBy: String, chargeEntity: ChargeEntity, sentenceTypeEntity: SentenceTypeEntity?, consecutiveTo: SentenceEntity?): SentenceEntity = SentenceEntity(
      sentenceUuid = UUID.randomUUID(),
      chargeNumber = sentence.chargeNumber,
      statusId = if (sentence.active) EntityStatus.ACTIVE else EntityStatus.INACTIVE,
      createdBy = createdBy,
      createdPrison = sentence.prisonId,
      supersedingSentence = null,
      charge = chargeEntity,
      sentenceServeType = if (consecutiveTo != null) "CONSECUTIVE" else "UNKNOWN",
      consecutiveTo = consecutiveTo,
      sentenceType = sentenceTypeEntity,
      convictionDate = null,
      legacyData = sentence.legacyData,
    )

    fun from(sentence: MigrationCreateSentence, createdBy: String, chargeEntity: ChargeEntity, sentenceTypeEntity: SentenceTypeEntity?, consecutiveTo: SentenceEntity?): SentenceEntity = SentenceEntity(
      sentenceUuid = UUID.randomUUID(),
      chargeNumber = sentence.chargeNumber,
      statusId = if (sentence.active) EntityStatus.ACTIVE else EntityStatus.INACTIVE,
      createdBy = createdBy,
      createdPrison = null,
      supersedingSentence = null,
      charge = chargeEntity,
      sentenceServeType = if (consecutiveTo != null) "CONSECUTIVE" else "UNKNOWN",
      consecutiveTo = consecutiveTo,
      sentenceType = sentenceTypeEntity,
      convictionDate = null,
      legacyData = sentence.legacyData,
    )
  }
}
