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
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateSentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
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
  var lifetimeSentenceUuid: UUID?,
  @Column
  var sentenceUuid: UUID,
  @Column
  val chargeNumber: String?,

  @Column
  @Enumerated(EnumType.ORDINAL)
  var statusId: EntityStatus,
  @Column
  val createdAt: ZonedDateTime = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS),
  @Column
  val createdByUsername: String,
  @Column
  val createdPrison: String?,
  @Column
  val sentenceServeType: String,
  @OneToOne
  @JoinColumn(name = "consecutive_to_id")
  val consecutiveTo: SentenceEntity?,
  @OneToOne
  @JoinColumn(name = "sentence_type_id")
  val sentenceType: SentenceTypeEntity,
  @OneToOne
  @JoinColumn(name = "superseding_sentence_id")
  var supersedingSentence: SentenceEntity?,
  @ManyToOne
  @JoinColumn(name = "charge_id")
  val charge: ChargeEntity,
  @Column
  val convictionDate: LocalDate?,

) {
  @OneToMany
  @JoinColumn(name = "sentence_id")
  var periodLengths: List<PeriodLengthEntity> = emptyList()

  @OneToOne(mappedBy = "sentenceEntity")
  var fineAmountEntity: FineAmountEntity? = null

  fun isSame(other: SentenceEntity?): Boolean {
    return chargeNumber == other?.chargeNumber &&
      periodLengths.all { periodLength -> other.periodLengths.any { otherPeriodLength -> periodLength.isSame(otherPeriodLength) } } &&
      sentenceServeType == other.sentenceServeType &&
      sentenceType == other.sentenceType &&
      ((consecutiveTo == null && other.consecutiveTo == null) || consecutiveTo?.isSame(other.consecutiveTo) == true) &&
      convictionDate == other.convictionDate &&
      ((fineAmountEntity == null && other.fineAmountEntity == null) || fineAmountEntity?.isSame(other.fineAmountEntity) == true)
  }

  companion object {
    fun from(sentence: CreateSentence, createdByUsername: String, chargeEntity: ChargeEntity, consecutiveTo: SentenceEntity?, sentenceType: SentenceTypeEntity): SentenceEntity {
      val sentenceEntity = SentenceEntity(
        lifetimeSentenceUuid = UUID.randomUUID(),
        sentenceUuid = sentence.sentenceUuid ?: UUID.randomUUID(),
        chargeNumber = sentence.chargeNumber,
        statusId = EntityStatus.ACTIVE,
        createdByUsername = createdByUsername,
        createdPrison = null,
        supersedingSentence = null,
        charge = chargeEntity,
        sentenceServeType = sentence.sentenceServeType,
        consecutiveTo = consecutiveTo,
        sentenceType = sentenceType,
        convictionDate = sentence.convictionDate,
      )
      sentenceEntity.periodLengths = sentence.periodLengths.map { PeriodLengthEntity.from(it) }
      sentence.fineAmount?.let { sentenceEntity.fineAmountEntity = FineAmountEntity.from(it) }
      return sentenceEntity
    }
  }
}
