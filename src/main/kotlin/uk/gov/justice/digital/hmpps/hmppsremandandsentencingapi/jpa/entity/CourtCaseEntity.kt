package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.ColumnResult
import jakarta.persistence.ConstructorResult
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.NamedAttributeNode
import jakarta.persistence.NamedEntityGraph
import jakarta.persistence.NamedNativeQuery
import jakarta.persistence.NamedSubgraph
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.SqlResultSetMapping
import jakarta.persistence.Table
import org.hibernate.annotations.BatchSize
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.DraftCreateCourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.PeriodLengthType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.projection.CourtCaseRow
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.CourtAppearanceLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.CourtCaseLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCreateCourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateCourtCase
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
@NamedNativeQuery(
  name = "CourtCaseEntity.searchCourtCases",
  query = """
      select cc.id as courtCaseId, 
        cc.prisoner_id as prisonerId, 
        cc.case_unique_identifier as courtCaseUuid, 
        cc.status_id as courtCaseStatus, 
        cc.legacy_data as courtCaseLegacyData, 
        appearanceData.appearance_count as appearanceCount, 
        appearanceData.case_references as caseReferences, 
        appearanceData.first_day_in_custody as firstDayInCustody,
        apl.years as appearancePeriodLengthYears,
        apl.months as appearancePeriodLengthMonths,
        apl.weeks as appearancePeriodLengthWeeks,
        apl.days as appearancePeriodLengthDays,
        apl.period_order as appearancePeriodLengthOrder,
        apl.period_length_type as appearancePeriodLengthType,
        nlca.court_code as nextCourtAppearanceCourtCode,
        ncaat.description as nextCourtAppearanceTypeDescription,
        nlca.appearance_date as nextCourtAppearanceDate,
        nlca.appearance_time as nextCourtAppearanceTime,
        lca.court_case_reference as latestCourtAppearanceCaseReference,
        lca.court_code as latestCourtAppearanceCourtCode,
        lca.appearance_date as latestCourtAppearanceDate,
        lca.warrant_type as latestCourtAppearanceWarrantType,
        ao.outcome_name as latestCourtAppearanceOutcome,
        lca.legacy_data as latestCourtAppearanceLegacyData,
        lca.overall_conviction_date as latestCourtAppearanceOverallConvictionDate
      from court_case cc
      join (select cc1.id, count(ca.id) as appearance_count, string_agg(ca.court_case_reference, ',') as case_references, min(ca.appearance_date) as first_day_in_custody 
        from court_case cc1
        join court_appearance ca on cc1.id = ca.court_case_id 
        join court_appearance lca1 on lca1.id = cc1.latest_court_appearance_id
        where ca.status_id = :appearanceStatus
          and cc1.status_id<>:courtCaseStatus
          and cc1.prisoner_id = :prisonerId
          and cc1.latest_court_appearance_id is not null
        group by cc1.id, lca1.appearance_date
        order by lca1.appearance_date desc
        limit :limit offset :offset) as appearanceData on appearanceData.id = cc.id
      join court_appearance lca on lca.id = cc.latest_court_appearance_id
      left join appearance_outcome ao on lca.appearance_outcome_id = ao.id
      left join period_length apl on apl.appearance_id=lca.id
      left join next_court_appearance nlca on nlca.id = lca.next_court_appearance_id
      left join appearance_type ncaat on ncaat.id = nlca.appearance_type_id
  """,
  resultSetMapping = "courtCaseRowMapping",
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
  val prisonerId: String,
  @Column
  val caseUniqueIdentifier: String,

  @Column
  val createdAt: ZonedDateTime = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS),
  @Column
  val createdBy: String,
  val createdPrison: String? = null,
  @Column
  @Enumerated(EnumType.ORDINAL)
  var statusId: EntityStatus,

  @JdbcTypeCode(SqlTypes.JSON)
  var legacyData: CourtCaseLegacyData? = null,

) {
  @OneToMany
  @JoinColumn(name = "court_case_id")
  @BatchSize(size = 50)
  var appearances: Set<CourtAppearanceEntity> = emptySet()

  @OneToMany(mappedBy = "courtCase")
  var draftAppearances: MutableList<DraftAppearanceEntity> = mutableListOf()

  @OneToOne
  @JoinColumn(name = "latest_court_appearance_id")
  var latestCourtAppearance: CourtAppearanceEntity? = null

  companion object {

    fun from(courtCase: CreateCourtCase, createdBy: String, caseUniqueIdentifier: String = UUID.randomUUID().toString()): CourtCaseEntity = CourtCaseEntity(prisonerId = courtCase.prisonerId, caseUniqueIdentifier = caseUniqueIdentifier, createdBy = createdBy, createdPrison = courtCase.prisonId, statusId = EntityStatus.ACTIVE, legacyData = courtCase.legacyData)

    fun from(courtCase: LegacyCreateCourtCase, createdByUsername: String): CourtCaseEntity = CourtCaseEntity(prisonerId = courtCase.prisonerId, caseUniqueIdentifier = UUID.randomUUID().toString(), createdBy = createdByUsername, statusId = if (courtCase.active) EntityStatus.ACTIVE else EntityStatus.INACTIVE)

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

    fun from(draftCourtCase: DraftCreateCourtCase, createdByUsername: String): CourtCaseEntity = CourtCaseEntity(prisonerId = draftCourtCase.prisonerId, caseUniqueIdentifier = UUID.randomUUID().toString(), createdBy = createdByUsername, statusId = EntityStatus.DRAFT)
  }
}
