package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.CourtDataIngestionApiClient
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.config.FeaturesConfig
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.HearingThingsToDoData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.ThingToDo
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
      val hearings = courtDataIngestionApi.getHearings(prisonerId)
      val warrantHearings = hearings.filter { hearing -> hearing.documents.any { it.isWarrant() } && hearing.caseReferences.size == 1 }

      val thingsToDo = warrantHearings
        .filter { warrantHearing ->
          val totalCases = courtCaseRepository.countCourtCasesByPrisonerAndCourtCaseReference(prisonerId, warrantHearing.caseReferences[0])
          totalCases == 0L
        }
        .map { warrantHearing ->
          ThingToDo(
            type = if (warrantHearing.isRemandHearing()) ThingToDoType.NEW_REMAND_WARRANT else ThingToDoType.NEW_SENTENCING_WARRANT,
            hearingThingsToDoData = HearingThingsToDoData(
              warrantHearing.hearingId,
              warrantHearing.caseReferences.first(),
              warrantHearing.hearingDate.toLocalDate(),
              warrantHearing.hearingType,
            ),
          )
        }

      return ThingsToDo(
        prisonerId = prisonerId,
        thingsToDo = thingsToDo,
      )
    }

    return ThingsToDo(
      prisonerId = prisonerId,
      thingsToDo = emptyList(),
    )
  }
}
