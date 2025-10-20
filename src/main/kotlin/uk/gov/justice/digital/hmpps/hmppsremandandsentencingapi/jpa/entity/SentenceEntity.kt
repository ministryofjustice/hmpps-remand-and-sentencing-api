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
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.PeriodLengthType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.RecallEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.projection.ConsecutiveToSentenceRow
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.projection.ViewSentenceRow
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.ChargeLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCreateSentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateSentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.PeriodLengthLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.SentenceLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.booking.BookingCreateSentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.merge.MergeCreateSentence
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
@SqlResultSetMapping(
  name = "viewSentenceRowMapping",
  classes = [
    ConstructorResult(
      targetClass = ViewSentenceRow::class,
      columns = arrayOf(
        ColumnResult(name = "sentenceUuid"),
        ColumnResult(name = "offenceCode"),
        ColumnResult(name = "offenceStartDate", type = LocalDate::class),
        ColumnResult(name = "offenceEndDate", type = LocalDate::class),
        ColumnResult(name = "dpsOffenceOutcome"),
        ColumnResult(name = "countNumber"),
        ColumnResult(name = "sentenceLegacyData", type = SentenceLegacyData::class),
        ColumnResult(name = "convictionDate", type = LocalDate::class),
        ColumnResult(name = "sentenceServeType"),
        ColumnResult(name = "consecutiveToSentenceUuid"),
        ColumnResult(name = "fineAmount", type = BigDecimal::class),
        ColumnResult(name = "courtCode"),
        ColumnResult(name = "courtCaseReference"),
        ColumnResult(name = "appearanceDate", type = LocalDate::class),
        ColumnResult(name = "chargeLegacyData", type = ChargeLegacyData::class),
        ColumnResult(name = "dpsSentenceType"),
        ColumnResult(name = "periodLengthUuid"),
        ColumnResult(name = "years"),
        ColumnResult(name = "months"),
        ColumnResult(name = "weeks"),
        ColumnResult(name = "days"),
        ColumnResult(name = "periodOrder"),
        ColumnResult(name = "periodLengthType", type = PeriodLengthType::class),
        ColumnResult(name = "periodLengthLegacyData", type = PeriodLengthLegacyData::class),
        ColumnResult(name = "mergedFromAppearanceId"),
        ColumnResult(name = "mergedFromCaseReference"),
        ColumnResult(name = "mergedFromCourtCode"),
        ColumnResult(name = "mergedFromWarrantDate", type = LocalDate::class),
        ColumnResult(name = "mergedFromDate", type = LocalDate::class),
      ),
    ),
  ],
)
@NamedNativeQuery(
  name = "SentenceEntity.findViewSentences",
  query = """
    select s.sentence_uuid as sentenceUuid, 
      c.offence_code as offenceCode,
      c.offence_start_date as offenceStartDate, 
      c.offence_end_date as offenceEndDate,
      co.outcome_name as dpsOffenceOutcome,
      s.count_number as countNumber,
      s.legacy_data as sentenceLegacyData,
      s.conviction_date as convictionDate,
      s.sentence_serve_type as sentenceServeType,
      cts.sentence_uuid as consecutiveToSentenceUuid,
      s.fine_amount as fineAmount,
      ca.court_code as courtCode, 
      ca.court_case_reference as courtCaseReference, 
      ca.appearance_date as appearanceDate, 
      c.legacy_data as chargeLegacyData,
      st.description as dpsSentenceType,
      pl.period_length_uuid as periodLengthUuid,
      pl.years as years,
      pl.months as months,
      pl.weeks as weeks,
      pl.days as days,
      pl.period_order as periodOrder,
      pl.period_length_type as periodLengthType,
      pl.legacy_data as periodLengthLegacyData,
      mca.id as mergedFromAppearanceId,
      mca.court_case_reference as mergedFromCaseReference,
      mca.court_code as mergedFromCourtCode,
      mca.appearance_date as mergedFromWarrantDate,
      c.merged_from_date as mergedFromDate
    from sentence s
    join charge c on s.charge_id = c.id
    left join charge_outcome co on co.id = c.charge_outcome_id
    join appearance_charge ac on ac.charge_id = c.id
    join court_appearance ca on ac.appearance_id = ca.id
    join court_case cc on ca.court_case_id = cc.id
    left join sentence cts on s.consecutive_to_id = cts.id
    left join sentence_type st on s.sentence_type_id = st.id 
    left join period_length pl on pl.sentence_id = s.id
    left join court_case mcc on mcc.id = c.merged_from_case_id
    left join court_appearance mca on mca.court_case_id = mcc.id
    where s.status_id in :sentenceStatuses
    and cc.prisoner_id = :prisonerId
    and c.status_id = :status
    and ca.status_id = :status
    and cc.status_id in :courtCaseStatuses
    and pl.status_id in :periodLengthStatuses
  """,
  resultSetMapping = "viewSentenceRowMapping",
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
  @Enumerated(EnumType.STRING)
  var statusId: SentenceEntityStatus,
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

  fun latestRecall(): RecallEntity? = recallSentences.map { it.recall }.filter { it.statusId == RecallEntityStatus.ACTIVE }.maxByOrNull { it.createdAt }

  fun copyFrom(sentence: CreateSentence, createdBy: String, chargeEntity: ChargeEntity, consecutiveTo: SentenceEntity?, sentenceType: SentenceTypeEntity?): SentenceEntity {
    val sentenceEntity = SentenceEntity(
      sentenceUuid = UUID.randomUUID(),
      countNumber = sentence.chargeNumber,
      statusId = SentenceEntityStatus.ACTIVE,
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

  fun copyFrom(
    sentence: LegacyCreateSentence,
    createdBy: String,
    consecutiveTo: SentenceEntity?,
    isManyCharges: Boolean,
  ): SentenceEntity {
    val newSentenceServeType =
      when {
        consecutiveTo != null -> "CONSECUTIVE"
        this.sentenceServeType == "FORTHWITH" -> "FORTHWITH"
        else -> "CONCURRENT"
      }

    val sentenceEntity = SentenceEntity(
      sentenceUuid = UUID.randomUUID(),
      statusId = if (isManyCharges) {
        SentenceEntityStatus.MANY_CHARGES_DATA_FIX
      } else if (sentence.active) {
        SentenceEntityStatus.ACTIVE
      } else {
        SentenceEntityStatus.INACTIVE
      },
      createdBy = createdBy,
      supersedingSentence = this,
      charge = charge,
      sentenceServeType = newSentenceServeType,
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
      statusId = SentenceEntityStatus.MANY_CHARGES_DATA_FIX,
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

  fun copyFrom(sentence: MergeCreateSentence, createdBy: String, chargeEntity: ChargeEntity, sentenceTypeEntity: SentenceTypeEntity?): SentenceEntity {
    sentence.legacyData.active = sentence.active
    return SentenceEntity(
      sentenceUuid = sentenceUuid,
      statusId = SentenceEntityStatus.MANY_CHARGES_DATA_FIX,
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
      statusId = SentenceEntityStatus.MANY_CHARGES_DATA_FIX,
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
    statusId = SentenceEntityStatus.DELETED
  }

  companion object {
    fun from(sentence: CreateSentence, createdBy: String, chargeEntity: ChargeEntity, consecutiveTo: SentenceEntity?, sentenceType: SentenceTypeEntity?): SentenceEntity {
      val sentenceEntity = SentenceEntity(
        sentenceUuid = sentence.sentenceUuid ?: UUID.randomUUID(),
        countNumber = sentence.chargeNumber,
        statusId = SentenceEntityStatus.ACTIVE,
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
        SentenceEntityStatus.MANY_CHARGES_DATA_FIX
      } else if (sentence.active) {
        SentenceEntityStatus.ACTIVE
      } else {
        SentenceEntityStatus.INACTIVE
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
        statusId = if (sentence.active) SentenceEntityStatus.ACTIVE else SentenceEntityStatus.INACTIVE,
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

    fun from(sentence: MergeCreateSentence, createdBy: String, chargeEntity: ChargeEntity, sentenceTypeEntity: SentenceTypeEntity?): SentenceEntity {
      sentence.legacyData.active = sentence.active
      return SentenceEntity(
        sentenceUuid = UUID.randomUUID(),
        statusId = if (sentence.active) SentenceEntityStatus.ACTIVE else SentenceEntityStatus.INACTIVE,
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
        statusId = SentenceEntityStatus.DUPLICATE,
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
