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
    val c1 = createCharge(sentenceRef = UUID.randomUUID().toString(), consecutiveToRef = null)
    val c2 = createCharge(sentenceRef = UUID.randomUUID().toString(), consecutiveToRef = c1.sentence!!.sentenceReference)
    val c3 = createCharge(sentenceRef = UUID.randomUUID().toString(), consecutiveToRef = c2.sentence!!.sentenceReference)
    val c4 = createCharge(sentenceRef = UUID.randomUUID().toString(), consecutiveToRef = c3.sentence!!.sentenceReference)

    val unorderedList = listOf(c4, c2, c3, c1)

    val sorted = courtAppearanceService.orderChargesByConsecutiveChain(unorderedList)

    val actual = sorted.map { it.sentence!!.sentenceReference }
    assertThat(actual).containsExactly(c1.sentence.sentenceReference, c2.sentence.sentenceReference, c3.sentence.sentenceReference, c4.sentence!!.sentenceReference)
  }

  @Test
  fun `should sort charges with two independent chains`() {
    val c1 = createCharge(sentenceRef = "1", consecutiveToRef = null)
    val c2 = createCharge(sentenceRef = "2", consecutiveToRef = "1")
    val c3 = createCharge(sentenceRef = "3", consecutiveToRef = "2")
    val c4 = createCharge(sentenceRef = "4", consecutiveToRef = "3")

    val c5 = createCharge(sentenceRef = "5", consecutiveToRef = null)
    val c6 = createCharge(sentenceRef = "6", consecutiveToRef = "5")
    val c7 = createCharge(sentenceRef = "7", consecutiveToRef = "6")
    val c8 = createCharge(sentenceRef = "8", consecutiveToRef = "7")

    val mixedList = listOf(c6, c2, c4, c5, c1, c8, c3, c7)

    val sorted = courtAppearanceService.orderChargesByConsecutiveChain(mixedList)
    val refs = sorted.map { it.sentence!!.sentenceReference }

    assertThat(refs).containsExactly("5", "1", "6", "2", "3", "7", "4", "8")
  }

  private fun createCharge(sentenceRef: String, consecutiveToRef: String?): CreateCharge = CreateCharge(
    appearanceUuid = null,
    offenceCode = "X",
    offenceStartDate = LocalDate.now(),
    offenceEndDate = null,
    outcomeUuid = null,
    terrorRelated = null,
    prisonId = "P",
    legacyData = null,
    sentence = CreateSentence(
      sentenceUuid = null,
      chargeNumber = sentenceRef,
      periodLengths = emptyList(),
      sentenceServeType = if (consecutiveToRef == null) "FORTHWITH" else "CONSECUTIVE",
      consecutiveToSentenceUuid = null,
      sentenceTypeId = null,
      convictionDate = null,
      fineAmount = null,
      prisonId = null,
      sentenceReference = sentenceRef,
      consecutiveToSentenceReference = consecutiveToRef,
    ),
  )
}
