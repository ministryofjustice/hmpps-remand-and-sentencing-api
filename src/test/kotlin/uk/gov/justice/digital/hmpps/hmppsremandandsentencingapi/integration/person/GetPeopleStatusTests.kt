package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.person

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.person.PeopleStatusRequest
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.wiremock.AdjustmentsApiExtension.Companion.adjustmentsApi
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator

class GetPeopleStatusTests : IntegrationTestBase() {

  @BeforeEach
  fun setUp() {
    adjustmentsApi.stubAllowCreateAdjustments()
    adjustmentsApi.stubGetAdjustmentsDefaultToNone()
  }

  @Test
  fun `should return remand and recall status for each valid prisoner`() {
    val remandPrisoner = DpsDataCreator.DEFAULT_PRISONER_ID
    val recallPrisoner = "PRI124"
    val notFoundPrisoner = "PRI125"
    createCourtCase(DpsDataCreator.dpsCreateNonSentencedCourtCase(prisonerId = remandPrisoner))
    val (_, sentencedCase) = createCourtCase(DpsDataCreator.dpsCreateCourtCase(prisonerId = recallPrisoner))
    val sentenceUuid = sentencedCase.appearances.first().charges.first().sentence!!.sentenceUuid
    createRecall(DpsDataCreator.dpsCreateRecall(prisonerId = recallPrisoner, sentenceIds = listOf(sentenceUuid)))

    postStatuses(setOf(remandPrisoner, recallPrisoner, notFoundPrisoner))
      .expectStatus().isOk
      .expectBody()
      .consumeWith { println(objectMapper.readTree(it.responseBody).toPrettyString()) }
      .jsonPath("$.peopleStatus.$remandPrisoner.isOnRemand").isEqualTo(true)
      .jsonPath("$.peopleStatus.$remandPrisoner.isCurrentlyOnRecall").isEqualTo(false)
      .jsonPath("$.peopleStatus.$recallPrisoner.isOnRemand").isEqualTo(false)
      .jsonPath("$.peopleStatus.$recallPrisoner.isCurrentlyOnRecall").isEqualTo(true)
      .jsonPath("$.peopleStatus.$notFoundPrisoner.isOnRemand").isEqualTo(false)
      .jsonPath("$.peopleStatus.$notFoundPrisoner.isCurrentlyOnRecall").isEqualTo(false)
  }

  @Test
  fun `should return unknown prisoners with both statuses false`() {
    postStatuses(setOf("PRISONER_NOT_FOUND"))
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.peopleStatus.PRISONER_NOT_FOUND.isOnRemand").isEqualTo(false)
      .jsonPath("$.peopleStatus.PRISONER_NOT_FOUND.isCurrentlyOnRecall").isEqualTo(false)
  }

  @Test
  fun `should return only requested prisoners`() {
    createCourtCase(DpsDataCreator.dpsCreateNonSentencedCourtCase(prisonerId = DpsDataCreator.DEFAULT_PRISONER_ID))
    createCourtCase(DpsDataCreator.dpsCreateNonSentencedCourtCase(prisonerId = "PRI124"))

    postStatuses(setOf(DpsDataCreator.DEFAULT_PRISONER_ID))
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.peopleStatus.PRI123.isOnRemand").isEqualTo(true)
      .jsonPath("$.peopleStatus.PRI124").doesNotExist()
  }

  @Test
  fun `should return unauthorised when no token`() {
    webTestClient.post()
      .uri("/person/statuses")
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(PeopleStatusRequest(setOf(DpsDataCreator.DEFAULT_PRISONER_ID)))
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `should return forbidden when invalid role`() {
    webTestClient.post()
      .uri("/person/statuses")
      .headers {
        it.authToken(roles = listOf("RANDOM_ROLE"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .bodyValue(PeopleStatusRequest(setOf(DpsDataCreator.DEFAULT_PRISONER_ID)))
      .exchange()
      .expectStatus().isForbidden
  }

  private fun postStatuses(prisonerIds: Set<String>) = webTestClient.post()
    .uri("/person/statuses")
    .headers {
      it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__PRISONER__RO"))
      it.contentType = MediaType.APPLICATION_JSON
    }
    .bodyValue(PeopleStatusRequest(prisonerIds))
    .exchange()
}
