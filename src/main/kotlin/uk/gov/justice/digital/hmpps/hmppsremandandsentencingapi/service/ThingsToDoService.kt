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
          val newRemandCaseSupported = newCourtCase && warrantHearing.isRemandHearing()
          val repeatRemandSupported = features.hmctsWarrantThingToDo.repeatRemandHearingEnabled &&
            !newCourtCase &&
            warrantHearing.isRemandHearing()
          val newSentencingCaseSupported = features.hmctsWarrantThingToDo.sentencingEnabled &&
            newCourtCase &&
            warrantHearing.isSentenceHearing()
          val thingToDoSupported = newRemandCaseSupported || newSentencingCaseSupported || repeatRemandSupported
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

      val multipleNotifications = thingsToDo.size > 1
      val multipleNotificationsSupported = multipleNotifications && features.hmctsWarrantThingToDo.multipleNotificationsEnabled
      if (!multipleNotifications || multipleNotificationsSupported) {
        return ThingsToDo(
          prisonerId = prisonerId,
          thingsToDo = thingsToDo,
        )
      }
    }

    return ThingsToDo(
      prisonerId = prisonerId,
      thingsToDo = emptyList(),
    )
  }
}
