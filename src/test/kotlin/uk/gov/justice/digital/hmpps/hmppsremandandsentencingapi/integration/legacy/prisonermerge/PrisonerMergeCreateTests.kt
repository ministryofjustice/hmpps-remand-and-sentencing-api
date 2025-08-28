package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.prisonermerge

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.PrisonerMergeDataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus

class PrisonerMergeCreateTests : IntegrationTestBase() {

  @Test
  fun `moved all records to new prisoner number`() {
    val migratedRecords = migrateCases(DataCreator.migrationCreateSentenceCourtCases())
    val retainedPrisonerNumber = "PRI999"
    val mergePerson = PrisonerMergeDataCreator.mergePerson()

    webTestClient
      .post()
      .uri("/legacy/court-case/merge/person/$retainedPrisonerNumber")
      .bodyValue(mergePerson)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_COURT_CASE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated

    val courtCaseUuid = migratedRecords.courtCases.first().courtCaseUuid
    webTestClient.get()
      .uri {
        it.path("/court-case/paged/search")
          .queryParam("prisonerId", retainedPrisonerNumber)
          .build()
      }
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.content.[0].courtCaseUuid")
      .isEqualTo(courtCaseUuid)
  }

  @Test
  fun `deactivate court case and sentence on merge`() {
    val migratedRecords = migrateCases(DataCreator.migrationCreateSentenceCourtCases())
    val retainedPrisonerNumber = "PRI999"
    val courtCaseUuid = migratedRecords.courtCases.first().courtCaseUuid
    val sentenceUuid = migratedRecords.sentences.first().sentenceUuid
    val mergePerson = PrisonerMergeDataCreator.mergePerson(
      casesDeactivated = listOf(PrisonerMergeDataCreator.deactivatedCourtCase(courtCaseUuid)),
      sentencesDeactivated = listOf(PrisonerMergeDataCreator.deactivatedSentence(sentenceUuid)),
    )

    webTestClient
      .post()
      .uri("/legacy/court-case/merge/person/$retainedPrisonerNumber")
      .bodyValue(mergePerson)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_COURT_CASE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated

    webTestClient.get()
      .uri {
        it.path("/court-case/paged/search")
          .queryParam("prisonerId", retainedPrisonerNumber)
          .build()
      }
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.content.[0].courtCaseStatus")
      .isEqualTo(EntityStatus.INACTIVE)

    val sentence = sentenceRepository.findBySentenceUuid(sentenceUuid).first()
    Assertions.assertThat(sentence.statusId).isEqualTo(EntityStatus.INACTIVE)
  }
}
