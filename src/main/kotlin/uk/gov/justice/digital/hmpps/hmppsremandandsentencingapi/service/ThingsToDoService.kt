package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.CourtDataIngestionApiClient
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.config.FeaturesConfig
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.HearingThingsToDoData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.HearingThingsToDoWarrantType
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
        .mapNotNull { warrantHearing ->
          val cases = courtCaseRepository.findCourtCasesByPrisonerAndCourtCaseReference(prisonerId, warrantHearing.caseReferences[0])
          val case = cases.maxByOrNull { it.appearances.maxOf { it.appearanceDate } }

          val newCourtCase = case == null
          val repeatRemand = features.hmctsWarrantThingToDo.repeatRemandHearingEnabled &&
            case != null &&
            warrantHearing.isRemandHearing()
          val thingToDoSupported = newCourtCase || repeatRemand
          if (!thingToDoSupported) {
            return@mapNotNull null
          }
          ThingToDo(
            type = ThingToDoType.NEW_WARRANT,
            hearingThingsToDoData = HearingThingsToDoData(
              hearingId = warrantHearing.hearingId,
              courtCaseReference = warrantHearing.caseReferences.first(),
              hearingDate = warrantHearing.hearingDate,
              hearingType = warrantHearing.hearingType,
              warrantType = if (warrantHearing.isRemandHearing()) HearingThingsToDoWarrantType.REMAND else HearingThingsToDoWarrantType.SENTENCING,
              courtCaseUuid = case?.caseUniqueIdentifier,
            ),
          )
        }.sortedByDescending {
          it.hearingThingsToDoData.hearingDate
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
