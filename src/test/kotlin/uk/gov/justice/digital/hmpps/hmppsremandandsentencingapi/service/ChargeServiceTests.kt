package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCharge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateSentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.RecordResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.AppearanceChargeHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.ChargeHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.ChargeOutcomeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.ChargeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.AppearanceChargeHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.ChargeHistoryRepository
import java.time.LocalDate
import java.util.UUID

class ChargeServiceTests {
  private val chargeRepository = mockk<ChargeRepository>(relaxed = true)
  private val chargeOutcomeRepository = mockk<ChargeOutcomeRepository>(relaxed = true)
  private val sentenceService = mockk<SentenceService>(relaxed = true)
  private val serviceUserService = mockk<ServiceUserService>(relaxed = true)
  private val chargeHistoryRepository = mockk<ChargeHistoryRepository>(relaxed = true)
  private val appearanceChargeHistoryRepository = mockk<AppearanceChargeHistoryRepository>(relaxed = true)

  private val chargeService = ChargeService(
    chargeRepository = chargeRepository,
    chargeOutcomeRepository = chargeOutcomeRepository,
    sentenceService = sentenceService,
    serviceUserService = serviceUserService,
    chargeHistoryRepository = chargeHistoryRepository,
    appearanceChargeHistoryRepository = appearanceChargeHistoryRepository,
  )

  @Test
  fun `createCharge inserts new charge when replacedChargeUuid is null`() {
    val savedCharge = mockk<ChargeEntity>(relaxed = true)
    val savedSentence = mockk<SentenceEntity>(relaxed = true)
    val courtAppearance = mockk<CourtAppearanceEntity>()
    val appearanceUuid = UUID.randomUUID()
    every { courtAppearance.appearanceUuid } returns appearanceUuid
    every { serviceUserService.getUsername() } returns "test-user"

    // ensure repository save returns the passed entity (avoid ClassCastException from default mock return)
    every { chargeHistoryRepository.save(any()) } answers { firstArg<ChargeHistoryEntity>() }
    every { appearanceChargeHistoryRepository.save(any()) } answers { firstArg<AppearanceChargeHistoryEntity>() }

    every { chargeRepository.findFirstByChargeUuidAndStatusIdNotOrderByUpdatedAtDesc(any()) } returns null
    every { chargeRepository.save(any()) } returns savedCharge
    every { sentenceService.createSentence(any(), any(), any(), any(), any(), any(), any()) } returns
      RecordResponse(savedSentence, mutableSetOf())

    val createCharge = CreateCharge(
      appearanceUuid = null,
      offenceCode = "X",
      offenceStartDate = LocalDate.now(),
      offenceEndDate = null,
      outcomeUuid = null,
      terrorRelated = null,
      foreignPowerRelated = null,
      domesticViolenceRelated = null,
      prisonId = "P",
      legacyData = null,
      sentence = CreateSentence(
        sentenceUuid = UUID.randomUUID(),
        chargeNumber = "1",
        periodLengths = emptyList(),
        sentenceServeType = "FORTHWITH",
        consecutiveToSentenceUuid = null,
        sentenceTypeId = null,
        convictionDate = null,
        fineAmount = null,
        prisonId = null,
      ),
      replacingChargeUuid = null,
    )

    val result = chargeService.createCharge(
      createCharge,
      mutableMapOf(),
      "A1234BC",
      "CASE-1",
      courtAppearance,
      false,
    )

    assertThat(result.record).isSameAs(savedCharge)
    verify { chargeRepository.save(any()) }
  }

  @Test
  fun `createCharge looks up superseding charge when replacedChargeUuid is provided`() {
    val replacedUuid = UUID.randomUUID()
    val supersedingCharge = mockk<ChargeEntity>(relaxed = true)
    val savedCharge = mockk<ChargeEntity>(relaxed = true)
    val savedSentence = mockk<SentenceEntity>(relaxed = true)
    val courtAppearance = mockk<CourtAppearanceEntity>()
    val appearanceUuid = UUID.randomUUID()
    every { courtAppearance.appearanceUuid } returns appearanceUuid
    every { serviceUserService.getUsername() } returns "test-user"

    // ensure repository save returns the passed entity (avoid ClassCastException)
    every { chargeHistoryRepository.save(any()) } answers { firstArg<ChargeHistoryEntity>() }
    every { appearanceChargeHistoryRepository.save(any()) } answers { firstArg<AppearanceChargeHistoryEntity>() }

    // Top-level existing-charge lookup returns null (create path)
    every { chargeRepository.findFirstByChargeUuidAndStatusIdNotOrderByUpdatedAtDesc(any()) } returns null
    // When createChargeEntity sees replacedChargeUuid it should query the repository for that UUID
    every { chargeRepository.findFirstByChargeUuidAndStatusIdNotOrderByUpdatedAtDesc(replacedUuid) } returns supersedingCharge
    every { chargeRepository.save(any()) } returns savedCharge
    every { sentenceService.createSentence(any(), any(), any(), any(), any(), any(), any()) } returns
      RecordResponse(savedSentence, mutableSetOf())

    val createCharge = CreateCharge(
      appearanceUuid = null,
      offenceCode = "Y",
      offenceStartDate = LocalDate.now(),
      offenceEndDate = null,
      outcomeUuid = null,
      terrorRelated = null,
      foreignPowerRelated = null,
      domesticViolenceRelated = null,
      prisonId = "P",
      legacyData = null,
      sentence = CreateSentence(
        sentenceUuid = UUID.randomUUID(),
        chargeNumber = "2",
        periodLengths = emptyList(),
        sentenceServeType = "CONSECUTIVE",
        consecutiveToSentenceUuid = null,
        sentenceTypeId = null,
        convictionDate = null,
        fineAmount = null,
        prisonId = null,
      ),
      replacingChargeUuid = replacedUuid,
    )

    val result = chargeService.createCharge(
      createCharge,
      mutableMapOf(),
      "B222BC",
      "CASE-2",
      courtAppearance,
      false,
    )

    assertThat(result.record).isSameAs(savedCharge)
    verify { chargeRepository.findFirstByChargeUuidAndStatusIdNotOrderByUpdatedAtDesc(replacedUuid) }
    verify { chargeRepository.save(any()) }
  }
}
