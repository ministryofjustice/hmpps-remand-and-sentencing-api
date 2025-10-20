package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.courtcase

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateCourtCasesResponse

@SpringBootTest(webEnvironment = RANDOM_PORT, properties = ["features.fix.many.charges.to.sentence=disabled"])
class NoManyChargesToSentenceFixTests : IntegrationTestBase() {

  @Test
  fun `many charges to a single sentence creates multiple sentence records`() {
    val sentence = DataCreator.migrationCreateSentence()
    val firstCharge = DataCreator.migrationCreateCharge(sentence = sentence)
    val secondCharge = DataCreator.migrationCreateCharge(chargeNOMISId = 1111, sentence = sentence)
    val appearance = DataCreator.migrationCreateCourtAppearance(charges = listOf(firstCharge, secondCharge))
    val courtCase = DataCreator.migrationCreateCourtCase(appearances = listOf(appearance))
    val courtCases = DataCreator.migrationCreateCourtCases(courtCases = listOf(courtCase))

    val response = webTestClient
      .post()
      .uri("/legacy/court-case/migration")
      .bodyValue(courtCases)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_COURT_CASE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated
      .returnResult(MigrationCreateCourtCasesResponse::class.java)
      .responseBody.blockFirst()!!

    val sentenceUuid = response.sentences.first { sentence.sentenceId == it.sentenceNOMISId }.sentenceUuid
    val periodLengthUuid = response.sentenceTerms.first { sentence.periodLengths.first().periodLengthId == it.sentenceTermNOMISId }.periodLengthUuid
    webTestClient
      .get()
      .uri {
        it.path("/court-case/paged/search")
          .queryParam("prisonerId", courtCases.prisonerId)
          .build()
      }
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.content[*].latestCourtAppearance.charges[*].sentence.sentenceUuid")
      .value<List<String>> { result ->
        Assertions.assertThat(result).allMatch { it == sentenceUuid.toString() }
      }
      .jsonPath("$.content[*].latestCourtAppearance.charges[*].sentence.periodLengths[*].periodLengthUuid")
      .value<List<String>> { result ->
        Assertions.assertThat(result).allMatch { it == periodLengthUuid.toString() }
      }

    domainQueueIsEmpty()
  }
}
