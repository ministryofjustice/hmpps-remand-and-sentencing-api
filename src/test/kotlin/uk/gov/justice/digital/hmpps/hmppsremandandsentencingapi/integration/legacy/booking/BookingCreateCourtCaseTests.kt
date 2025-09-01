package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.booking

import org.assertj.core.api.Assertions
import org.hamcrest.Matchers.everyItem
import org.hamcrest.core.IsNull
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.BookingDataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.booking.BookingCreateCourtCasesResponse
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import java.util.regex.Pattern

class BookingCreateCourtCaseTests : IntegrationTestBase() {

  @Test
  fun `create all entities and return ids against NOMIS ids`() {
    val bookingCourtCases = BookingDataCreator.bookingCreateCourtCases(
      courtCases = listOf(
        BookingDataCreator.bookingCreateCourtCase(
          appearances = listOf(BookingDataCreator.bookingCreateCourtAppearance(charges = listOf(BookingDataCreator.bookingCreateCharge(sentence = BookingDataCreator.bookingCreateSentence())))),
        ),
      ),
    )
    val response = webTestClient
      .post()
      .uri("/legacy/court-case/booking")
      .bodyValue(bookingCourtCases)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_COURT_CASE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated
      .returnResult(BookingCreateCourtCasesResponse::class.java)
      .responseBody.blockFirst()!!
    Assertions.assertThat(response.courtCases).hasSize(bookingCourtCases.courtCases.size)
    val courtCaseResponse = response.courtCases.first()

    Assertions.assertThat(courtCaseResponse.courtCaseUuid).matches(Pattern.compile("([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})"))
    Assertions.assertThat(response.appearances).hasSize(bookingCourtCases.courtCases.flatMap { it.appearances }.size)
    val createdAppearance = response.appearances.first()
    Assertions.assertThat(createdAppearance.eventId).isEqualTo(bookingCourtCases.courtCases.first().appearances.first().eventId)
    Assertions.assertThat(response.charges).hasSize(bookingCourtCases.courtCases.flatMap { it.appearances.flatMap { it.charges } }.size)
    val createdCharge = response.charges.first()
    Assertions.assertThat(createdCharge.chargeNOMISId).isEqualTo(bookingCourtCases.courtCases.first().appearances.first().charges.first().chargeNOMISId)
    val createdSentence = response.sentences.first()

    Assertions.assertThat(createdSentence.sentenceNOMISId).isEqualTo(bookingCourtCases.courtCases.first().appearances.first().charges.first().sentence!!.sentenceId)

    val messagesOnQueue = getMessages(5)
    Assertions.assertThat(messagesOnQueue).extracting<String> { it.eventType }.containsExactlyInAnyOrder("court-case.inserted", "court-appearance.inserted", "charge.inserted", "sentence.inserted", "sentence.period-length.inserted")
    Assertions.assertThat(messagesOnQueue).extracting<String> { it.additionalInformation.get("source").asText() }.allMatch { it.equals("NOMIS") }
  }

  @Test
  fun `can create snapshots of charges in different appearances`() {
    val chargeNOMISId = 555L
    val firstSnapshot = BookingDataCreator.bookingCreateCharge(chargeNOMISId = chargeNOMISId, legacyData = DataCreator.chargeLegacyData(nomisOutcomeCode = "99"), offenceEndDate = LocalDate.now().plusDays(5), sentence = null)
    val secondSnapshot = BookingDataCreator.bookingCreateCharge(chargeNOMISId = chargeNOMISId, legacyData = DataCreator.chargeLegacyData(nomisOutcomeCode = "66"), offenceStartDate = null, sentence = null)
    val firstAppearance = BookingDataCreator.bookingCreateCourtAppearance(eventId = 1, appearanceDate = LocalDate.now().minusDays(7), legacyData = DataCreator.courtAppearanceLegacyData(), charges = listOf(firstSnapshot))
    val secondAppearance = BookingDataCreator.bookingCreateCourtAppearance(eventId = 2, appearanceDate = LocalDate.now().minusDays(2), legacyData = DataCreator.courtAppearanceLegacyData(), charges = listOf(secondSnapshot))
    val bookingCourtCase = BookingDataCreator.bookingCreateCourtCase(appearances = listOf(secondAppearance, firstAppearance))
    val bookingCourtCases = BookingDataCreator.bookingCreateCourtCases(courtCases = listOf(bookingCourtCase))
    val response = webTestClient
      .post()
      .uri("/legacy/court-case/booking")
      .bodyValue(bookingCourtCases)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_COURT_CASE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated
      .returnResult(BookingCreateCourtCasesResponse::class.java)
      .responseBody.blockFirst()!!
    Assertions.assertThat(response.charges).hasSize(1)
    val chargeLifetimeUuid = response.charges.first().chargeUuid
    val firstAppearanceUuid = response.appearances.first { appearanceResponse -> firstAppearance.eventId == appearanceResponse.eventId }.appearanceUuid
    checkChargeSnapshotOutcomeCode(firstAppearanceUuid, chargeLifetimeUuid, firstSnapshot.legacyData.nomisOutcomeCode!!)
    val secondAppearanceUuid = response.appearances.first { appearanceResponse -> secondAppearance.eventId == appearanceResponse.eventId }.appearanceUuid
    checkChargeSnapshotOutcomeCode(secondAppearanceUuid, chargeLifetimeUuid, secondSnapshot.legacyData.nomisOutcomeCode!!)
  }

  @Test
  fun `create snapshots of charges where difference a sentence exists`() {
    val chargeNOMISId = 555L
    val firstSnapshot = BookingDataCreator.bookingCreateCharge(chargeNOMISId = chargeNOMISId, legacyData = DataCreator.chargeLegacyData(nomisOutcomeCode = "99"), offenceEndDate = LocalDate.now().plusDays(5), sentence = null)
    val secondSnapshot = firstSnapshot.copy(sentence = BookingDataCreator.bookingCreateSentence())
    val firstAppearance = BookingDataCreator.bookingCreateCourtAppearance(eventId = 1, appearanceDate = LocalDate.now().minusDays(7), legacyData = DataCreator.courtAppearanceLegacyData(), charges = listOf(firstSnapshot))
    val secondAppearance = BookingDataCreator.bookingCreateCourtAppearance(eventId = 2, appearanceDate = LocalDate.now().minusDays(2), legacyData = DataCreator.courtAppearanceLegacyData(), charges = listOf(secondSnapshot))
    val bookingCourtCase = BookingDataCreator.bookingCreateCourtCase(appearances = listOf(secondAppearance, firstAppearance))
    val bookingCourtCases = BookingDataCreator.bookingCreateCourtCases(courtCases = listOf(bookingCourtCase))
    val response = webTestClient
      .post()
      .uri("/legacy/court-case/booking")
      .bodyValue(bookingCourtCases)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_COURT_CASE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated
      .returnResult(BookingCreateCourtCasesResponse::class.java)
      .responseBody.blockFirst()!!
    val courtCaseUuid = response.courtCases.first().courtCaseUuid
    val chargeUuid = response.charges.first().chargeUuid
    val firstAppearanceUuid = response.appearances.first { appearanceResponse -> firstAppearance.eventId == appearanceResponse.eventId }.appearanceUuid
    val secondAppearanceUuid = response.appearances.first { appearanceResponse -> secondAppearance.eventId == appearanceResponse.eventId }.appearanceUuid
    webTestClient
      .get()
      .uri("/court-case/$courtCaseUuid")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.appearances[?(@.appearanceUuid == '$firstAppearanceUuid')].charges[?(@.chargeUuid == '$chargeUuid')].sentence")
      .value(everyItem(IsNull.nullValue()))
      .jsonPath("$.appearances[?(@.appearanceUuid == '$secondAppearanceUuid')].charges[?(@.chargeUuid == '$chargeUuid')].sentence")
      .value(everyItem(IsNull.notNullValue()))
  }

  @Test
  fun `creates DPS next court appearances when next court date and appearance date match`() {
    val futureAppearance = BookingDataCreator.bookingCreateCourtAppearance(eventId = 567, appearanceDate = LocalDate.now().plusDays(7), legacyData = DataCreator.courtAppearanceLegacyData(nomisOutcomeCode = null, outcomeDescription = null, nextEventDateTime = null))
    val firstAppearance = BookingDataCreator.bookingCreateCourtAppearance(legacyData = DataCreator.courtAppearanceLegacyData(nextEventDateTime = futureAppearance.appearanceDate.atTime(10, 0)))
    val bookingCourtCase = BookingDataCreator.bookingCreateCourtCase(appearances = listOf(firstAppearance, futureAppearance))
    val bookingCourtCases = BookingDataCreator.bookingCreateCourtCases(courtCases = listOf(bookingCourtCase))
    val response = webTestClient
      .post()
      .uri("/legacy/court-case/booking")
      .bodyValue(bookingCourtCases)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_COURT_CASE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated
      .returnResult(BookingCreateCourtCasesResponse::class.java)
      .responseBody.blockFirst()!!

    val firstAppearanceUuid = response.appearances.first { appearanceResponse -> firstAppearance.eventId == appearanceResponse.eventId }.appearanceUuid
    val firstAppearanceEntity = courtAppearanceRepository.findByAppearanceUuid(firstAppearanceUuid)
    Assertions.assertThat(firstAppearanceEntity!!.nextCourtAppearance!!.courtCode).isEqualTo(futureAppearance.courtCode)
  }

  @Test
  fun `create DPS next court appearance for latest past appearance when no matching next court date or appearance date`() {
    val futureAppearance = BookingDataCreator.bookingCreateCourtAppearance(eventId = 567, appearanceDate = LocalDate.now().plusDays(10), legacyData = DataCreator.courtAppearanceLegacyData(nomisOutcomeCode = null, outcomeDescription = null, nextEventDateTime = null))
    val firstAppearance = BookingDataCreator.bookingCreateCourtAppearance(legacyData = DataCreator.courtAppearanceLegacyData(nextEventDateTime = LocalDateTime.now().plusDays(5)))
    val bookingCourtCase = BookingDataCreator.bookingCreateCourtCase(appearances = listOf(firstAppearance, futureAppearance))
    val bookingCourtCases = BookingDataCreator.bookingCreateCourtCases(courtCases = listOf(bookingCourtCase))
    val response = webTestClient
      .post()
      .uri("/legacy/court-case/booking")
      .bodyValue(bookingCourtCases)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_COURT_CASE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated
      .returnResult(BookingCreateCourtCasesResponse::class.java)
      .responseBody.blockFirst()!!

    val firstAppearanceUuid = response.appearances.first { appearanceResponse -> firstAppearance.eventId == appearanceResponse.eventId }.appearanceUuid
    val firstAppearanceEntity = courtAppearanceRepository.findByAppearanceUuid(firstAppearanceUuid)!!
    Assertions.assertThat(firstAppearanceEntity.nextCourtAppearance!!.courtCode).isEqualTo(futureAppearance.courtCode)
  }

  @Test
  fun `many charges to a single sentence creates multiple sentence records`() {
    val sentence = BookingDataCreator.bookingCreateSentence()
    val firstCharge = BookingDataCreator.bookingCreateCharge(sentence = sentence)
    val secondCharge = BookingDataCreator.bookingCreateCharge(chargeNOMISId = 1111, sentence = sentence)
    val appearance = BookingDataCreator.bookingCreateCourtAppearance(charges = listOf(firstCharge, secondCharge))
    val courtCase = BookingDataCreator.bookingCreateCourtCase(appearances = listOf(appearance))
    val courtCases = BookingDataCreator.bookingCreateCourtCases(courtCases = listOf(courtCase))

    val response = webTestClient
      .post()
      .uri("/legacy/court-case/booking")
      .bodyValue(courtCases)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_COURT_CASE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated
      .returnResult(BookingCreateCourtCasesResponse::class.java)
      .responseBody.blockFirst()!!

    val sentenceUuid = response.sentences.first { sentence.sentenceId == it.sentenceNOMISId }.sentenceUuid
    val periodLengthUuid = response.sentenceTerms.first { sentence.periodLengths.first().periodLengthId == it.sentenceTermNOMISId }.periodLengthUuid
    val sentences = sentenceRepository.findAll()
    Assertions.assertThat(sentences).extracting<UUID> { it.sentenceUuid }.allMatch { it.equals(sentenceUuid) }
    val periodLengths = periodLengthRepository.findAll()
    Assertions.assertThat(periodLengths).extracting<UUID> { it.periodLengthUuid }.allMatch { it.equals(periodLengthUuid) }
  }

  private fun checkChargeSnapshotOutcomeCode(appearanceLifetimeUuid: UUID, chargeLifetimeUuid: UUID, expectedOutcomeCode: String) {
    webTestClient
      .get()
      .uri("/legacy/court-appearance/$appearanceLifetimeUuid/charge/$chargeLifetimeUuid")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_APPEARANCE_RO"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.nomisOutcomeCode")
      .isEqualTo(expectedOutcomeCode)
  }

  @Test
  fun `no token results in unauthorized`() {
    val bookingCourtCase = BookingDataCreator.bookingCreateCourtCase()
    webTestClient
      .post()
      .uri("/legacy/court-case/booking")
      .bodyValue(bookingCourtCase)
      .headers {
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `token with incorrect role is forbidden`() {
    val bookingCourtCases = BookingDataCreator.bookingCreateCourtCases()
    webTestClient
      .post()
      .uri("/legacy/court-case/booking")
      .bodyValue(bookingCourtCases)
      .headers {
        it.authToken(roles = listOf("ROLE_OTHER_FUNCTION"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isForbidden
  }
}
