package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.custom

import jakarta.persistence.EntityManager
import org.springframework.data.jpa.repository.JpaContext
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtCaseEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.AppearanceDateSortDirection
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.projection.CourtCaseRow

class CourtCaseSearchRepositoryImpl : CourtCaseSearchRepository {

  private val entityManager: EntityManager

  constructor(context: JpaContext) {
    this.entityManager = context.getEntityManagerByManagedType(CourtCaseEntity::class.java)
  }
  override fun searchCourtCases(
    prisonerId: String,
    limit: Int,
    offset: Long,
    appearanceDateSortDirection: AppearanceDateSortDirection,
    appearanceStatus: EntityStatus,
    courtCaseStatus: EntityStatus,
  ): List<CourtCaseRow> = entityManager.createNativeQuery(searchQuery.replace("<appearanceDateSortDirection>", appearanceDateSortDirection.name), "courtCaseRowMapping")
    .setParameter("prisonerId", prisonerId)
    .setParameter("limit", limit)
    .setParameter("offset", offset)
    .setParameter("appearanceStatus", appearanceStatus)
    .setParameter("courtCaseStatus", courtCaseStatus)
    .resultList as List<CourtCaseRow>

  companion object {
    val searchQuery = """
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
        st.classification as sentenceTypeClassification,
        spl.id as sentencePeriodLengthId,
        spl.period_length_uuid as sentencePeriodLengthUuid,
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
        order by lca1.appearance_date <appearanceDateSortDirection>
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
    """.trimIndent()
  }
}
