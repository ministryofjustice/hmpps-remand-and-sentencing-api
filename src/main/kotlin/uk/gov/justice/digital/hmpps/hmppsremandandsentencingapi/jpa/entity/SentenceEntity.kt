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
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateSentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

@Entity
@Table(name = "sentence")
data class SentenceEntity(
  @Id
  @Column
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Int = 0,
  @Column
  var lifetimeSentenceUuid: UUID?,
  @Column
  var sentenceUuid: UUID,
  @Column
  val chargeNumber: String,
  @OneToOne
  @JoinColumn(name = "custodial_length_id")
  var custodialPeriodLength: PeriodLengthEntity,
  @OneToOne
  @JoinColumn(name = "extended_licence_length_id")
  var extendedLicensePeriodLength: PeriodLengthEntity?,
  @Column
  @Enumerated(EnumType.ORDINAL)
  var statusId: EntityStatus,
  @Column
  val createdAt: ZonedDateTime = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS),
  @Column
  val createdByUsername: String,
  @Column
  val createdPrison: String?,
  @OneToOne
  @JoinColumn(name = "superseding_sentence_id")
  var supersedingSentence: SentenceEntity?,
  @ManyToOne
  @JoinColumn(name = "charge_id")
  val charge: ChargeEntity,
) {

  fun isSame(other: SentenceEntity?): Boolean {
    return chargeNumber == other?.chargeNumber &&
      custodialPeriodLength.isSame(other.custodialPeriodLength) &&
      ((extendedLicensePeriodLength == null && other.extendedLicensePeriodLength == null) || extendedLicensePeriodLength?.isSame(other.extendedLicensePeriodLength) == true)
  }

  companion object {
    fun from(sentence: CreateSentence, createdByUsername: String, chargeEntity: ChargeEntity): SentenceEntity {
      return SentenceEntity(
        lifetimeSentenceUuid = UUID.randomUUID(),
        sentenceUuid = sentence.sentenceUuid ?: UUID.randomUUID(),
        chargeNumber = sentence.chargeNumber,
        custodialPeriodLength = PeriodLengthEntity.from(sentence.custodialPeriodLength),
        extendedLicensePeriodLength = sentence.extendedLicensePeriodLength?.let { PeriodLengthEntity.from(it) },
        statusId = EntityStatus.ACTIVE,
        createdByUsername = createdByUsername,
        createdPrison = null,
        supersedingSentence = null,
        charge = chargeEntity,
      )
    }
  }
}
