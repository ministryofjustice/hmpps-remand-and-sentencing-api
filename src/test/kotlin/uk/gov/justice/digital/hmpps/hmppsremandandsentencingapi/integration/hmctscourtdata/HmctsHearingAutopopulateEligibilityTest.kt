package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.hmctscourtdata

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.HmctsCourHearingDocument
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.HmctsCourtHearing
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.typeReference
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.HmctsAutopopulateFeature
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.HmctsAutopopulateFeatureType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.HmctsHearingAutopopulateEligibility
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.wiremock.CourtDataIngestionApiExtension
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.HmctsHearingIdPair
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import java.time.LocalDate
import java.util.UUID

class HmctsHearingAutopopulateEligibilityTest : IntegrationTestBase() {

  @Test
  fun `Test multiple hearings are eligible for autopopulate`() {
    val prisonerOne = "ABCD123"
    val caseReferenceOne = "CASE123"
    val prisonerOneHearingOne = REMAND_HEARING.copy(hearingId = UUID.randomUUID(), caseReferences = listOf(caseReferenceOne))
    val prisonerOneHearingTwo = SENTENCING_HEARING.copy(hearingId = UUID.randomUUID(), caseReferences = listOf("CASE345", "CASE678"))
    val prisonerTwo = "ABCD456"
    val prisonerTwoHearingOne = REMAND_HEARING.copy(hearingId = UUID.randomUUID())
    val prisonerTwoHearingTwo = SENTENCING_HEARING.copy(hearingId = UUID.randomUUID())
    CourtDataIngestionApiExtension.courtDataIngestionApi.stubCourtHearing(
      prisonerOneHearingOne,
      prisonerOne,
    )
    CourtDataIngestionApiExtension.courtDataIngestionApi.stubCourtHearing(
      prisonerOneHearingTwo,
      prisonerOne,
    )
    CourtDataIngestionApiExtension.courtDataIngestionApi.stubCourtHearing(
      prisonerTwoHearingOne,
      prisonerTwo,
    )
    CourtDataIngestionApiExtension.courtDataIngestionApi.stubCourtHearing(
      prisonerTwoHearingTwo,
      prisonerTwo,
    )
    val (courtCaseId, _) = createCourtCase(
      DpsDataCreator.dpsCreateCourtCase(
        prisonerId = prisonerOne,
        appearances = listOf(
          DpsDataCreator.dpsCreateCourtAppearance(
            courtCaseReference = caseReferenceOne,
          ),
        ),
      ),
    )

    val response = webTestClient
      .post()
      .uri("/hmcts-court-data/hearing-autopopulate-eligibility")
      .bodyValue(
        listOf(
          HmctsHearingIdPair(prisonerOne, prisonerOneHearingOne.hearingId),
          HmctsHearingIdPair(prisonerOne, prisonerOneHearingTwo.hearingId),
          HmctsHearingIdPair(prisonerTwo, prisonerTwoHearingOne.hearingId),
          HmctsHearingIdPair(prisonerTwo, prisonerTwoHearingTwo.hearingId),
        ),
      )
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_SENTENCE_RO"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus().isOk
      .expectBody(typeReference<List<HmctsHearingAutopopulateEligibility>>())
      .returnResult().responseBody!!

    Assertions.assertThat(response).isEqualTo(
      listOf(
        HmctsHearingAutopopulateEligibility(

          existingCaseIdentifier = courtCaseId,
          features = listOf(
            HmctsAutopopulateFeature(type = HmctsAutopopulateFeatureType.NEW_REMAND_APPEARANCE_ON_EXISTING_CASE, enabled = true),
          ),
          hasWarrantAndPcr = true,
          hasBeenCompleted = false,
        ),
        HmctsHearingAutopopulateEligibility(existingCaseIdentifier = null, features = listOf(HmctsAutopopulateFeature(type = HmctsAutopopulateFeatureType.MULTIPLE_CASE_REFERENCES, enabled = false)), hasWarrantAndPcr = true, hasBeenCompleted = false),
        HmctsHearingAutopopulateEligibility(existingCaseIdentifier = null, features = listOf(HmctsAutopopulateFeature(type = HmctsAutopopulateFeatureType.REMAND_WARRANT, enabled = true)), hasWarrantAndPcr = true, hasBeenCompleted = false),
        HmctsHearingAutopopulateEligibility(existingCaseIdentifier = null, features = listOf(HmctsAutopopulateFeature(type = HmctsAutopopulateFeatureType.SENTENCING_WARRANT, enabled = true)), hasWarrantAndPcr = true, hasBeenCompleted = false),
      ),
    )
  }

  companion object {
    val HMCTS_HEARING_ID = UUID.randomUUID()
    val DOCUMENT_ID = UUID.randomUUID()
    val SENTENCING_WARRANT = HmctsCourHearingDocument(
      "SENTENCING_WARRANT",
      DOCUMENT_ID,
    )
    val REMAND_WARRANT = HmctsCourHearingDocument(
      "REMAND_WARRANT",
      DOCUMENT_ID,
    )
    val PCR = HmctsCourHearingDocument(
      "PRISON_COURT_REGISTER",
      DOCUMENT_ID,
    )
    val HEARING = HmctsCourtHearing(
      hearingId = HMCTS_HEARING_ID,
      courtName = "My court",
      courtId = UUID.randomUUID(),
      hearingDate = LocalDate.of(2026, 1, 1),
      caseReferences = listOf("ABC123"),
      hearingType = "First hearing",
      documents = emptyList(),
    )
    val SENTENCING_HEARING = HEARING.copy(
      documents = listOf(SENTENCING_WARRANT, PCR),
    )
    val REMAND_HEARING = HEARING.copy(
      documents = listOf(REMAND_WARRANT, PCR),
    )
  }
}
