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
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.ChargeLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.CourtAppearanceLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.CourtCaseLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCreateCourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateCourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.PeriodLengthLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.SentenceLegacyData
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
        nlca.id as nextCourtAppearanceId,
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
        lca.overall_conviction_date as latestCourtAppearanceOverallConvictionDate,
        c.id as chargeId,
        c.status_id as chargeStatus,
        c.offence_code as chargeOffenceCode,
        c.offence_start_date as chargeOffenceStartDate,
        c.offence_end_date as chargeOffenceEndDate,
        co.outcome_uuid as chargeOutcomeUuid,
        co.outcome_name as chargeOutcomeName,
        c.legacy_data as chargeLegacyData,
        s.id as sentenceId,
        s.sentence_uuid as sentenceUuid,
        s.charge_number as sentenceChargeNumber,
        s.status_id as sentenceStatus,
        s.sentence_serve_type as sentenceServeType,
        s.conviction_date as sentenceConvictionDate,
        s.legacy_data as sentenceLegacyData,
        s.fine_amount as sentenceFineAmount,
        cts.sentence_uuid as sentenceConsecutiveToUuid,
        st.sentence_type_uuid as sentenceTypeUuid,
        st.description as sentenceTypeDescription,
        spl.id as sentencePeriodLengthId,
        spl.status_id as sentencePeriodLengthStatus,
        spl.years as sentencePeriodLengthYears,
        spl.months as sentencePeriodLengthMonths,
        spl.weeks as sentencePeriodLengthWeeks,
        spl.days as sentencePeriodLengthDays,
        spl.period_order as sentencePeriodLengthOrder,
        spl.period_length_type as sentencePeriodLengthType,
        spl.legacy_data as sentencePeriodLengthLegacyData,
        rs.id as recallSentenceId
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
        order by lca1.appearance_date :appearanceDateSortDirection
        limit :limit offset :offset) as appearanceData on appearanceData.id = cc.id
      join court_appearance lca on lca.id = cc.latest_court_appearance_id
      left join appearance_outcome ao on lca.appearance_outcome_id = ao.id
      left join period_length apl on apl.appearance_id=lca.id
      left join next_court_appearance nlca on nlca.id = lca.next_court_appearance_id
      left join appearance_type ncaat on ncaat.id = nlca.appearance_type_id
      left join appearance_charge ac on ac.appearance_id = lca.id
      left join charge c on ac.charge_id = c.id
      left join charge_outcome co on c.charge_outcome_id = co.id
      left join sentence s on s.charge_id = c.id
      left join sentence cts on s.consecutive_to_id = cts.id
      left join sentence_type st on s.sentence_type_id = st.id
      left join period_length spl on spl.sentence_id = s.id
      left join recall_sentence rs on rs.sentence_id = s.id
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
        ColumnResult(name = "chargeStatus", EntityStatus::class),
        ColumnResult(name = "chargeOffenceCode"),
        ColumnResult(name = "chargeOffenceStartDate", type = LocalDate::class),
        ColumnResult(name = "chargeOffenceEndDate", type = LocalDate::class),
        ColumnResult(name = "chargeOutcomeUuid"),
        ColumnResult(name = "chargeOutcomeName"),
        ColumnResult(name = "chargeLegacyData", type = ChargeLegacyData::class),
        ColumnResult(name = "sentenceId"),
        ColumnResult(name = "sentenceUuid"),
        ColumnResult(name = "sentenceChargeNumber"),
        ColumnResult(name = "sentenceStatus", type = EntityStatus::class),
        ColumnResult(name = "sentenceServeType"),
        ColumnResult(name = "sentenceConvictionDate", type = LocalDate::class),
        ColumnResult(name = "sentenceLegacyData", type = SentenceLegacyData::class),
        ColumnResult(name = "sentenceFineAmount", type = BigDecimal::class),
        ColumnResult(name = "sentenceConsecutiveToUuid"),
        ColumnResult(name = "sentenceTypeUuid"),
        ColumnResult(name = "sentenceTypeDescription"),
        ColumnResult(name = "sentencePeriodLengthId"),
        ColumnResult(name = "sentencePeriodLengthStatus", type = EntityStatus::class),
        ColumnResult(name = "sentencePeriodLengthYears"),
        ColumnResult(name = "sentencePeriodLengthMonths"),
        ColumnResult(name = "sentencePeriodLengthWeeks"),
        ColumnResult(name = "sentencePeriodLengthDays"),
        ColumnResult(name = "sentencePeriodLengthOrder"),
        ColumnResult(name = "sentencePeriodLengthType", type = PeriodLengthType::class),
        ColumnResult(name = "sentencePeriodLengthLegacyData", type = PeriodLengthLegacyData::class),
        ColumnResult(name = "recallSentenceId"),
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
