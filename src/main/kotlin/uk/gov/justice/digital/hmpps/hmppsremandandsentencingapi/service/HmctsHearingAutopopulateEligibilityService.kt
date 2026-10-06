package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.CourtDataIngestionApiClient
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.HmctsCourtHearing
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.config.FeaturesConfig
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.HmctsAutopopulateFeature
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.HmctsAutopopulateFeatureType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.HmctsHearingAutopopulateEligibility
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtCaseRepository
import java.util.UUID

@Component
class HmctsHearingAutopopulateEligibilityService(
  val courtDataIngestionApi: CourtDataIngestionApiClient,
  val courtCaseRepository: CourtCaseRepository,
  val featuresConfig: FeaturesConfig,
) {
  @Transactional(readOnly = true)
  fun areHmctsHearingsEligibleForAutopopulate(hmctsHearingIdPair: List<HmctsHearingIdPair>): List<HmctsHearingAutopopulateEligibility> = hmctsHearingIdPair.map {
    val hearing = courtDataIngestionApi.getCourtHearing(it.hearingId, it.prisonerNumber)
    isHmctsHearingEligibleForAutopopulate(hearing, it.prisonerNumber)
  }

  @Transactional(readOnly = true)
  fun isHmctsHearingEligibleForAutopopulate(hearing: HmctsCourtHearing, prisonerNumber: String): HmctsHearingAutopopulateEligibility {
    val hasWarrantAndPcr = hearing.documents.any { it.isWarrant() } && hearing.documents.any { it.isPcr() }
    if (!hasWarrantAndPcr) {
      return HmctsHearingAutopopulateEligibility(
        features = emptyList(),
        hasBeenCompleted = false,
        hasWarrantAndPcr = false,
      )
    }

    if (hearing.caseReferences.size > 1) {
      return HmctsHearingAutopopulateEligibility(listOf(HmctsAutopopulateFeature(HmctsAutopopulateFeatureType.MULTIPLE_CASE_REFERENCES, false)))
    }

    val cases =
      courtCaseRepository.findCourtCasesByPrisonerAndCourtCaseReference(prisonerNumber, hearing.caseReferences[0])
    val case = cases.maxByOrNull { it.appearances.maxOf { it.appearanceDate } }
    val rasHearing = case?.appearances?.find { it.appearanceDate == hearing.hearingDate }

    if (rasHearing != null) {
      return HmctsHearingAutopopulateEligibility(
        features = listOf(),
        hasBeenCompleted = true,
      )
    }

    val features = mutableListOf<HmctsAutopopulateFeature>()

    val newCourtCase = case == null

    if (newCourtCase) {
      if (hearing.isRemandHearing()) {
        features.add(HmctsAutopopulateFeature(HmctsAutopopulateFeatureType.REMAND_WARRANT, true))
      } else if (hearing.isSentenceHearing()) {
        features.add(
          HmctsAutopopulateFeature(
            HmctsAutopopulateFeatureType.SENTENCING_WARRANT,
            featuresConfig.hmctsWarrantThingToDo.sentencingEnabled,
          ),
        )
      }
    } else {
      if (hearing.isRemandHearing()) {
        features.add(
          HmctsAutopopulateFeature(
            HmctsAutopopulateFeatureType.NEW_REMAND_APPEARANCE_ON_EXISTING_CASE,
            featuresConfig.hmctsWarrantThingToDo.repeatRemandHearingEnabled,
          ),
        )
      } else if (hearing.isSentenceHearing()) {
        features.add(
          HmctsAutopopulateFeature(
            HmctsAutopopulateFeatureType.NEW_SENTENCING_APPEARANCE_ON_EXISTING_CASE,
            false,
          ),
        )
      }
    }
    return HmctsHearingAutopopulateEligibility(
      features,
      existingCaseIdentifier = case?.caseUniqueIdentifier,
    )
  }
}

data class HmctsHearingIdPair(
  val prisonerNumber: String,
  val hearingId: UUID,
)
