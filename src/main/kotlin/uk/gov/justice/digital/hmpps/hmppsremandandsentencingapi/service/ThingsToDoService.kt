package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.CourtDataIngestionApiClient
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.config.FeaturesConfig
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.HearingThingsToDoData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.HearingThingsToDoWarrantType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.ThingToDo
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.ThingToDoType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.ThingsToDo

@Service
class ThingsToDoService(
  val courtDataIngestionApi: CourtDataIngestionApiClient,
  val hmctsHearingAutopopulateEligibilityService: HmctsHearingAutopopulateEligibilityService,
  val features: FeaturesConfig,
) {

  @Transactional(readOnly = true)
  fun getThingsToDo(prisonerId: String): ThingsToDo {
    if (features.hmctsWarrantThingToDo.enabled) {
      val hearings = courtDataIngestionApi.getHearings(prisonerId)

      val thingsToDo = hearings
        .mapNotNull { hearing ->
          val hearingEligibility = hmctsHearingAutopopulateEligibilityService.isHmctsHearingEligibleForAutopopulate(
            hearing,
            prisonerId,
          )
          if (hearingEligibility.hasWarrantAndPcr && !hearingEligibility.hasBeenCompleted && hearingEligibility.features.all { it.enabled }) {
            ThingToDo(
              type = ThingToDoType.NEW_WARRANT,
              hearingThingsToDoData = HearingThingsToDoData(
                hearingId = hearing.hearingId,
                courtCaseReference = hearing.caseReferences.first(),
                hearingDate = hearing.hearingDate,
                hearingType = hearing.hearingType,
                warrantType = if (hearing.isRemandHearing()) HearingThingsToDoWarrantType.REMAND else HearingThingsToDoWarrantType.SENTENCING,
                courtCaseUuid = hearingEligibility.cases.firstOrNull()?.caseUniqueIdentifier,
              ),
            )
          } else {
            null
          }
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
