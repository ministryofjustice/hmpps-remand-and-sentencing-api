package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.DocumentManagementApiClient
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCharge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateSentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.AppearanceOutcomeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.AppearanceTypeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtAppearanceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtCaseRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.NextCourtAppearanceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.AppearanceChargeHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.CourtAppearanceHistoryRepository
import java.time.LocalDate
import java.util.UUID

class CourtAppearanceServiceTests {
  private val courtCaseRepository = mockk<CourtCaseRepository>()
  private val courtAppearanceRepository = mockk<CourtAppearanceRepository>()
  private val serviceUserService = mockk<ServiceUserService>()
  private val courtAppearanceHistoryRepository = mockk<CourtAppearanceHistoryRepository>()
  private val nextCourtAppearanceRepository = mockk<NextCourtAppearanceRepository>()
  private val appearanceOutcomeRepository = mockk<AppearanceOutcomeRepository>()
  private val periodLengthService = mockk<PeriodLengthService>()
  private val chargeService = mockk<ChargeService>()
  private val documentManagementApiClient = mockk<DocumentManagementApiClient>()
  private val appearanceTypeRepository = mockk<AppearanceTypeRepository>()
  private val appearanceChargeHistoryRepository = mockk<AppearanceChargeHistoryRepository>()
  private val fixManyChargesToSentenceService = mockk<FixManyChargesToSentenceService>()
  private val documentService = mockk<UploadedDocumentService>()
  private val courtAppearanceService = CourtAppearanceService(
    courtAppearanceRepository = courtAppearanceRepository,
    nextCourtAppearanceRepository = nextCourtAppearanceRepository,
    appearanceOutcomeRepository = appearanceOutcomeRepository,
    periodLengthService = periodLengthService,
    chargeService = chargeService,
    serviceUserService = serviceUserService,
    courtCaseRepository = courtCaseRepository,
    documentManagementApiClient = documentManagementApiClient,
    appearanceTypeRepository = appearanceTypeRepository,
    courtAppearanceHistoryRepository = courtAppearanceHistoryRepository,
    appearanceChargeHistoryRepository = appearanceChargeHistoryRepository,
    fixManyChargesToSentenceService = fixManyChargesToSentenceService,
    documentService = documentService,
  )

  @Test
  fun `should sort charges when passed in order is mixed`() {
    val c1 = createCharge(sentenceUuid = UUID.randomUUID(), consecutiveToUuid = null)
    val c2 = createCharge(sentenceUuid = UUID.randomUUID(), consecutiveToUuid = c1.sentence!!.sentenceUuid)
    val c3 = createCharge(sentenceUuid = UUID.randomUUID(), consecutiveToUuid = c2.sentence!!.sentenceUuid)
    val c4 = createCharge(sentenceUuid = UUID.randomUUID(), consecutiveToUuid = c3.sentence!!.sentenceUuid)

    val unorderedList = listOf(c4, c2, c3, c1)

    val sorted = courtAppearanceService.orderChargesByConsecutiveChain(unorderedList)

    val actual = sorted.map { it.sentence!!.sentenceUuid }
    assertThat(actual).containsExactly(c1.sentence.sentenceUuid, c2.sentence.sentenceUuid, c3.sentence.sentenceUuid, c4.sentence!!.sentenceUuid)
  }

  @Test
  fun `should sort charges with two independent chains`() {
    val c1 = createCharge(sentenceUuid = uuid(1), consecutiveToUuid = null)
    val c2 = createCharge(sentenceUuid = uuid(2), consecutiveToUuid = uuid(1))
    val c3 = createCharge(sentenceUuid = uuid(3), consecutiveToUuid = uuid(2))
    val c4 = createCharge(sentenceUuid = uuid(4), consecutiveToUuid = uuid(3))

    val c5 = createCharge(sentenceUuid = uuid(5), consecutiveToUuid = null)
    val c6 = createCharge(sentenceUuid = uuid(6), consecutiveToUuid = uuid(5))
    val c7 = createCharge(sentenceUuid = uuid(7), consecutiveToUuid = uuid(6))
    val c8 = createCharge(sentenceUuid = uuid(8), consecutiveToUuid = uuid(7))

    val mixedList = listOf(c6, c2, c4, c5, c1, c8, c3, c7)

    val sorted = courtAppearanceService.orderChargesByConsecutiveChain(mixedList)
    val refs = sorted.map { it.sentence!!.sentenceUuid }

    assertThat(refs).containsExactly(uuid(5), uuid(1), uuid(6), uuid(2), uuid(3), uuid(7), uuid(4), uuid(8))
  }

  private fun uuid(i: Long) = UUID(0L, i)

  //  private fun createCharge(sentenceRef: String, consecutiveToRef: String?): CreateCharge = CreateCharge(
  private fun createCharge(sentenceUuid: UUID, consecutiveToUuid: UUID?): CreateCharge = CreateCharge(
    appearanceUuid = null,
    offenceCode = "X",
    offenceStartDate = LocalDate.now(),
    offenceEndDate = null,
    outcomeUuid = null,
    terrorRelated = null,
    prisonId = "P",
    legacyData = null,
    sentence = CreateSentence(
      sentenceUuid = sentenceUuid,
      chargeNumber = "1",
      periodLengths = emptyList(),
      sentenceServeType = if (consecutiveToUuid == null) "FORTHWITH" else "CONSECUTIVE",
      consecutiveToSentenceUuid = consecutiveToUuid,
      sentenceTypeId = null,
      convictionDate = null,
      fineAmount = null,
      prisonId = null,
    ),
  )
}
