package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.CourtDataIngestionApiClient
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.config.FeaturesConfig
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.HearingThingsToDoData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.ThingToDoType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.ThingsToDo
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtCaseRepository

@Service
class ThingsToDoService(
  val courtDataIngestionApi: CourtDataIngestionApiClient,
  val courtCaseRepository: CourtCaseRepository,
  val features: FeaturesConfig,
) {
  fun getThingsToDo(prisonerId: String): ThingsToDo {
    if (features.hmctsWarrantThingToDo.enabled) {
      val totalCases = courtCaseRepository.countCourtCasesByPrisoner(prisonerId)
      if (totalCases == 0L) {
        val hearings = courtDataIngestionApi.getHearings(prisonerId)
        val remandHearing = hearings.filter { hearing -> hearing.documents.any { it.documentType == "REMAND_WARRANT" } && hearing.caseReferences.size == 1 }
          .maxByOrNull { it.hearingDate }
        if (remandHearing != null) {
          return ThingsToDo(
            prisonerId = prisonerId,
            thingsToDo = listOf(ThingToDoType.NEW_REMAND_WARRANT),
            hearingThingsToDoData = HearingThingsToDoData(
              remandHearing.hearingId,
              remandHearing.caseReferences.first(),
            ),
          )
        }
      }
    }

    return ThingsToDo(
      prisonerId = prisonerId,
      thingsToDo = emptyList(),
      hearingThingsToDoData = null,
    )
  }
}
