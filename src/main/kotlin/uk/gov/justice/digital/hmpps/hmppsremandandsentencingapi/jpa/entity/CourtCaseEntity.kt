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
import jakarta.persistence.NamedAttributeNode
import jakarta.persistence.NamedEntityGraph
import jakarta.persistence.NamedSubgraph
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.SqlResultSetMapping
import jakarta.persistence.Table
import org.hibernate.annotations.BatchSize
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.PeriodLengthType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceTypeClassification
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.projection.CourtCaseRow
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.ChargeLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.CourtAppearanceLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.CourtCaseLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCreateCourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateCourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.PeriodLengthLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.SentenceLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.booking.BookingCreateCourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.merge.MergeCreateCourtCase
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

@Entity
@Table(name = "court_case")
@NamedEntityGraph(
  name = "CourtCaseEntity.withAppearancesAndOutcomes",
  attributeNodes = [
    NamedAttributeNode("appearances", subgraph = "appearanceDetails"),
    NamedAttributeNode("latestCourtAppearance", subgraph = "appearanceDetails"),
  ],
  subgraphs = [
    NamedSubgraph(
      name = "appearanceDetails",
      attributeNodes = [
        NamedAttributeNode("appearanceOutcome"),
        NamedAttributeNode("periodLengths"),
        NamedAttributeNode("nextCourtAppearance", subgraph = "nextAppearanceDetails"),
        NamedAttributeNode("appearanceCharges", subgraph = "appearanceChargeChargeDetails"),
      ],
    ),
    NamedSubgraph(
      name = "nextAppearanceDetails",
      attributeNodes = [
        NamedAttributeNode("appearanceType"),
      ],
    ),
    NamedSubgraph(
      name = "appearanceChargeChargeDetails",
      attributeNodes = [
        NamedAttributeNode("charge", subgraph = "chargeDetails"),
      ],
    ),
    NamedSubgraph(
      name = "chargeDetails",
      attributeNodes = [
        NamedAttributeNode("chargeOutcome"),
        NamedAttributeNode("sentences", subgraph = "sentenceDetails"),
      ],
    ),
    NamedSubgraph(
      name = "sentenceDetails",
      attributeNodes = [
        NamedAttributeNode("sentenceType"),
        NamedAttributeNode("consecutiveTo"),
        NamedAttributeNode("periodLengths"),
      ],
    ),
  ],
)
@SqlResultSetMapping(
  name = "courtCaseRowMapping",
  classes = [
    ConstructorResult(
      targetClass = CourtCaseRow::class,
      columns = arrayOf(
        ColumnResult(name = "courtCaseId"),
        ColumnResult(name = "prisonerId"),
        ColumnResult(name = "courtCaseUuid"),
        ColumnResult(name = "courtCaseStatus", type = EntityStatus::class),
        ColumnResult(name = "courtCaseLegacyData", type = CourtCaseLegacyData::class),
        ColumnResult(name = "appearanceCount"),
        ColumnResult(name = "caseReferences"),
        ColumnResult(name = "firstDayInCustody", type = LocalDate::class),
        ColumnResult(name = "appearancePeriodLengthYears"),
        ColumnResult(name = "appearancePeriodLengthMonths"),
        ColumnResult(name = "appearancePeriodLengthWeeks"),
        ColumnResult(name = "appearancePeriodLengthDays"),
        ColumnResult(name = "appearancePeriodLengthOrder"),
        ColumnResult(name = "appearancePeriodLengthType", type = PeriodLengthType::class),
        ColumnResult(name = "nextCourtAppearanceId"),
        ColumnResult(name = "nextCourtAppearanceCourtCode"),
        ColumnResult(name = "nextCourtAppearanceTypeDescription"),
        ColumnResult(name = "nextCourtAppearanceDate", type = LocalDate::class),
        ColumnResult(name = "nextCourtAppearanceTime", type = LocalTime::class),
        ColumnResult(name = "latestCourtAppearanceCaseReference"),
        ColumnResult(name = "latestCourtAppearanceCourtCode"),
        ColumnResult(name = "latestCourtAppearanceDate", type = LocalDate::class),
        ColumnResult(name = "latestCourtAppearanceWarrantType"),
        ColumnResult(name = "latestCourtAppearanceOutcome"),
        ColumnResult(name = "latestCourtAppearanceLegacyData", type = CourtAppearanceLegacyData::class),
        ColumnResult(name = "latestCourtAppearanceOverallConvictionDate", type = LocalDate::class),
        ColumnResult(name = "chargeId"),
        ColumnResult(name = "chargeUuid"),
        ColumnResult(name = "chargeStatus", EntityStatus::class),
        ColumnResult(name = "chargeOffenceCode"),
        ColumnResult(name = "chargeOffenceStartDate", type = LocalDate::class),
        ColumnResult(name = "chargeOffenceEndDate", type = LocalDate::class),
        ColumnResult(name = "chargeOutcomeUuid"),
        ColumnResult(name = "chargeOutcomeName"),
        ColumnResult(name = "chargeLegacyData", type = ChargeLegacyData::class),
        ColumnResult(name = "sentenceId"),
        ColumnResult(name = "sentenceUuid"),
        ColumnResult(name = "sentenceCountNumber"),
        ColumnResult(name = "sentenceStatus", type = EntityStatus::class),
        ColumnResult(name = "sentenceServeType"),
        ColumnResult(name = "sentenceConvictionDate", type = LocalDate::class),
        ColumnResult(name = "sentenceLegacyData", type = SentenceLegacyData::class),
        ColumnResult(name = "sentenceFineAmount", type = BigDecimal::class),
        ColumnResult(name = "sentenceConsecutiveToUuid"),
        ColumnResult(name = "sentenceTypeUuid"),
        ColumnResult(name = "sentenceTypeDescription"),
        ColumnResult(name = "sentenceTypeClassification", type = SentenceTypeClassification::class),
        ColumnResult(name = "sentencePeriodLengthId"),
        ColumnResult(name = "sentencePeriodLengthUuid"),
        ColumnResult(name = "sentencePeriodLengthStatus", type = EntityStatus::class),
        ColumnResult(name = "sentencePeriodLengthYears"),
        ColumnResult(name = "sentencePeriodLengthMonths"),
        ColumnResult(name = "sentencePeriodLengthWeeks"),
        ColumnResult(name = "sentencePeriodLengthDays"),
        ColumnResult(name = "sentencePeriodLengthOrder"),
        ColumnResult(name = "sentencePeriodLengthType", type = PeriodLengthType::class),
        ColumnResult(name = "sentencePeriodLengthLegacyData", type = PeriodLengthLegacyData::class),
        ColumnResult(name = "recallSentenceId"),
        ColumnResult(name = "chargeMergedFromDate", type = LocalDate::class),
        ColumnResult(name = "mergedFromCaseId"),
        ColumnResult(name = "mergedFromAppearanceId"),
        ColumnResult(name = "mergedFromCaseReference"),
        ColumnResult(name = "mergedFromCourtCode"),
        ColumnResult(name = "mergedFromWarrantDate", type = LocalDate::class),
        ColumnResult(name = "courtAppearanceId"),
        ColumnResult(name = "recallInAppearanceId"),
        ColumnResult(name = "mergedToCaseId"),
        ColumnResult(name = "mergedToDate", type = LocalDate::class),
        ColumnResult(name = "mergedToAppearanceId"),
        ColumnResult(name = "mergedToCaseReference"),
        ColumnResult(name = "mergedToCourtCode"),
        ColumnResult(name = "mergedToWarrantDate", type = LocalDate::class),
        ColumnResult(name = "futureSkeletonAppearanceUuid"),
      ),
    ),
  ],
)
class CourtCaseEntity(
  @Id
  @Column
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Int = 0,

  @Column
  var prisonerId: String,
  @Column
  val caseUniqueIdentifier: String,

  @Column
  val createdAt: ZonedDateTime = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS),
  @Column
  val createdBy: String,

  @Column
  var updatedAt: ZonedDateTime = createdAt,
  @Column
  var updatedBy: String? = null,

  val createdPrison: String? = null,
  @Column
  @Enumerated(EnumType.STRING)
  var statusId: EntityStatus,

  @JdbcTypeCode(SqlTypes.JSON)
  var legacyData: CourtCaseLegacyData? = null,

) {
  @OneToMany
  @JoinColumn(name = "court_case_id")
  @BatchSize(size = 50)
  var appearances: Set<CourtAppearanceEntity> = emptySet()

  @OneToOne
  @JoinColumn(name = "latest_court_appearance_id")
  var latestCourtAppearance: CourtAppearanceEntity? = null

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "merged_to_case_id")
  var mergedToCase: CourtCaseEntity? = null

  var mergedToDate: LocalDate? = null

  fun delete(username: String) {
    statusId = EntityStatus.DELETED
    updatedAt = ZonedDateTime.now()
    updatedBy = username
  }

  companion object {

    fun from(courtCase: CreateCourtCase, createdBy: String, caseUniqueIdentifier: String = UUID.randomUUID().toString()): CourtCaseEntity = CourtCaseEntity(prisonerId = courtCase.prisonerId, caseUniqueIdentifier = caseUniqueIdentifier, createdBy = createdBy, createdPrison = courtCase.prisonId, statusId = EntityStatus.ACTIVE, legacyData = courtCase.legacyData)

    fun from(courtCase: LegacyCreateCourtCase, createdByUsername: String): CourtCaseEntity = CourtCaseEntity(prisonerId = courtCase.prisonerId, caseUniqueIdentifier = UUID.randomUUID().toString(), createdBy = createdByUsername, statusId = if (courtCase.active) EntityStatus.ACTIVE else EntityStatus.INACTIVE, legacyData = courtCase.legacyData)

    fun from(migrationCreateCourtCase: MigrationCreateCourtCase, createdByUsername: String, prisonerId: String): CourtCaseEntity = CourtCaseEntity(
      prisonerId = prisonerId,
      caseUniqueIdentifier = UUID.randomUUID().toString(),
      createdBy = createdByUsername,
      statusId = if (migrationCreateCourtCase.merged) {
        EntityStatus.MERGED
      } else if (migrationCreateCourtCase.active) {
        EntityStatus.ACTIVE
      } else {
        EntityStatus.INACTIVE
      },
      legacyData = migrationCreateCourtCase.courtCaseLegacyData,
    )

    fun from(mergeCreateCourtCase: MergeCreateCourtCase, createdByUsername: String, prisonerId: String): CourtCaseEntity = CourtCaseEntity(
      prisonerId = prisonerId,
      caseUniqueIdentifier = UUID.randomUUID().toString(),
      createdBy = createdByUsername,
      statusId = if (mergeCreateCourtCase.merged) {
        EntityStatus.MERGED
      } else if (mergeCreateCourtCase.active) {
        EntityStatus.ACTIVE
      } else {
        EntityStatus.INACTIVE
      },
      legacyData = mergeCreateCourtCase.courtCaseLegacyData,
    )

    fun from(bookingCreateCourtCase: BookingCreateCourtCase, createdByUsername: String, prisonerId: String): CourtCaseEntity = CourtCaseEntity(
      prisonerId = prisonerId,
      caseUniqueIdentifier = UUID.randomUUID().toString(),
      createdBy = createdByUsername,
      statusId = EntityStatus.DUPLICATE,
      legacyData = bookingCreateCourtCase.courtCaseLegacyData,
    )
  }
}
