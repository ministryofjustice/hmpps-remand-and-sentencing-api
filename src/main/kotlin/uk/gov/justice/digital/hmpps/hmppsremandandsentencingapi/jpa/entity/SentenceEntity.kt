package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.ColumnResult
import jakarta.persistence.ConstructorResult
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.NamedNativeQuery
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.SqlResultSetMapping
import jakarta.persistence.Table
import org.hibernate.annotations.Formula
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateSentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.projection.ConsecutiveToSentenceRow
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.ChargeLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCreateSentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateSentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.SentenceLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.booking.BookingCreateSentence
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID
@SqlResultSetMapping(
  name = "consecutiveToSentenceRowMapping",
  classes = [
    ConstructorResult(
      targetClass = ConsecutiveToSentenceRow::class,
      columns = arrayOf(
        ColumnResult(name = "prisonerId"),
        ColumnResult(name = "caseUniqueIdentifier"),
        ColumnResult(name = "appearanceUuid"),
        ColumnResult(name = "courtCode"),
        ColumnResult(name = "courtCaseReference"),
        ColumnResult(name = "appearanceDate", type = LocalDate::class),
        ColumnResult(name = "chargeUuid"),
        ColumnResult(name = "offenceCode"),
        ColumnResult(name = "offenceStartDate", type = LocalDate::class),
        ColumnResult(name = "offenceEndDate", type = LocalDate::class),
        ColumnResult(name = "sentenceUuid"),
        ColumnResult(name = "countNumber"),
        ColumnResult(name = "chargeLegacyData", type = ChargeLegacyData::class),
      ),
    ),
  ],
)
@NamedNativeQuery(
  name = "SentenceEntity.findConsecutiveToSentences",
  query = """
  select cc.prisoner_id as prisonerId, cc.case_unique_identifier as caseUniqueIdentifier, ca.appearance_uuid as appearanceUuid, ca.court_code as courtCode, ca.court_case_reference as courtCaseReference, ca.appearance_date as appearanceDate, c.charge_uuid as chargeUuid, c.offence_code as offenceCode, c.offence_start_date as offenceStartDate, c.offence_end_date as offenceEndDate, s.sentence_uuid as sentenceUuid, s.count_number as countNumber, c.legacy_data as chargeLegacyData 
    from sentence s
    join charge c on s.charge_id = c.id
    join appearance_charge ac on ac.charge_id = c.id
    join court_appearance ca on ac.appearance_id = ca.id
    join court_case cc on ca.court_case_id = cc.id
    where s.status_id in :sentenceStatuses
    and cc.prisoner_id = :prisonerId
    and c.status_id = :status
    and ca.status_id = :status
    and ca.appearance_date <= :beforeOrOnAppearanceDate
    and cc.status_id = :status
    and s.legacy_data ->> 'bookingId' = :bookingId
""",
  resultSetMapping = "consecutiveToSentenceRowMapping",
)
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
  var countNumber: String? = null,
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

  fun isSame(other: SentenceEntity?): Boolean = countNumber == other?.countNumber &&
    sentenceServeType == other?.sentenceServeType &&
    sentenceType == other.sentenceType &&
    createdPrison == other.createdPrison &&
    ((consecutiveTo == null && other.consecutiveTo == null) || consecutiveTo?.isSame(other.consecutiveTo) == true) &&
    convictionDate == other.convictionDate &&
    ((fineAmount == null && other.fineAmount == null) || (fineAmount != null && other.fineAmount?.compareTo(fineAmount) == 0)) &&
    statusId == other.statusId &&
    ((legacyData == null && other.legacyData == null) || legacyData?.isSame(other.legacyData) == true)

  fun latestRecall(): RecallEntity? = recallSentences.map { it.recall }.filter { it.statusId == EntityStatus.ACTIVE }.maxByOrNull { it.createdAt }

  fun copyFrom(sentence: CreateSentence, createdBy: String, chargeEntity: ChargeEntity, consecutiveTo: SentenceEntity?, sentenceType: SentenceTypeEntity?): SentenceEntity {
    val sentenceEntity = SentenceEntity(
      sentenceUuid = UUID.randomUUID(),
      countNumber = sentence.chargeNumber,
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
      legacyData = legacyData,
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
      countNumber = countNumber,
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

  fun copyFrom(sentence: BookingCreateSentence, createdBy: String, chargeEntity: ChargeEntity, sentenceTypeEntity: SentenceTypeEntity?): SentenceEntity {
    sentence.legacyData.active = sentence.active
    return SentenceEntity(
      sentenceUuid = sentenceUuid,
      statusId = EntityStatus.DUPLICATE,
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
    countNumber = sentence.countNumber
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
    fun from(sentence: CreateSentence, createdBy: String, chargeEntity: ChargeEntity, consecutiveTo: SentenceEntity?, sentenceType: SentenceTypeEntity?): SentenceEntity {
      val sentenceEntity = SentenceEntity(
        sentenceUuid = sentence.sentenceUuid ?: UUID.randomUUID(),
        countNumber = sentence.chargeNumber,
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
      countNumber: String? = null,
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
      countNumber = countNumber,
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

    fun from(sentence: BookingCreateSentence, createdBy: String, chargeEntity: ChargeEntity, sentenceTypeEntity: SentenceTypeEntity?): SentenceEntity {
      sentence.legacyData.active = sentence.active
      return SentenceEntity(
        sentenceUuid = UUID.randomUUID(),
        statusId = EntityStatus.DUPLICATE,
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
