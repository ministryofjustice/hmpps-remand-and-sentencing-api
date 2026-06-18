package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.AggravatingFactorEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeAggravatingFactorEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.AggravatingFactorStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ChargeEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.AggravatingFactorRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.ChargeRepository
import java.time.ZonedDateTime
import java.util.UUID

class AggravatingFactorsServiceTest {

  private val aggravatingFactorRepository = mockk<AggravatingFactorRepository>(relaxed = true)
  private val chargeRepository = mockk<ChargeRepository>(relaxed = true)

  private val service = AggravatingFactorsService(
    aggravatingFactorRepository = aggravatingFactorRepository,
    chargeRepository = chargeRepository,
  )

  @BeforeEach
  fun setUp() {
    every { chargeRepository.saveAndFlush(any()) } answers { firstArg() }
  }

  @Test
  fun `clears existing aggravating factors and flushes before adding new ones`() {
    val charge = createCharge(terrorRelated = true)
    val terrorFactor = createAggravatingFactorEntity(1, "OATC")
    every { aggravatingFactorRepository.findByCodeIn(listOf("OATC")) } returns listOf(terrorFactor)

    service.replaceAggravatingFactors(charge)

    verify { chargeRepository.saveAndFlush(charge) }
  }

  @Test
  fun `adds OATC aggravating factor when terrorRelated is true`() {
    val charge = createCharge(terrorRelated = true)
    val terrorFactor = createAggravatingFactorEntity(1, "OATC")
    every { aggravatingFactorRepository.findByCodeIn(listOf("OATC")) } returns listOf(terrorFactor)

    service.replaceAggravatingFactors(charge)

    assertThat(charge.chargeAggravatingFactors).hasSize(1)
    assertThat(charge.chargeAggravatingFactors.first().aggravatingFactor.code).isEqualTo("OATC")
  }

  @Test
  fun `adds OAFPC aggravating factor when foreignPowerRelated is true`() {
    val charge = createCharge(foreignPowerRelated = true)
    val foreignPowerFactor = createAggravatingFactorEntity(2, "OAFPC")
    every { aggravatingFactorRepository.findByCodeIn(listOf("OAFPC")) } returns listOf(foreignPowerFactor)

    service.replaceAggravatingFactors(charge)

    assertThat(charge.chargeAggravatingFactors).hasSize(1)
    assertThat(charge.chargeAggravatingFactors.first().aggravatingFactor.code).isEqualTo("OAFPC")
  }

  @Test
  fun `adds both OATC and OAFPC when both flags are true`() {
    val charge = createCharge(terrorRelated = true, foreignPowerRelated = true)
    val terrorFactor = createAggravatingFactorEntity(1, "OATC")
    val foreignPowerFactor = createAggravatingFactorEntity(2, "OAFPC")
    val codesSlot = slot<List<String>>()
    every { aggravatingFactorRepository.findByCodeIn(capture(codesSlot)) } returns listOf(terrorFactor, foreignPowerFactor)

    service.replaceAggravatingFactors(charge)

    assertThat(codesSlot.captured).containsExactlyInAnyOrder("OATC", "OAFPC")
    assertThat(charge.chargeAggravatingFactors).hasSize(2)
    assertThat(charge.chargeAggravatingFactors.map { it.aggravatingFactor.code })
      .containsExactlyInAnyOrder("OATC", "OAFPC")
  }

  @Test
  fun `does not add any factors when both flags are false`() {
    val charge = createCharge(terrorRelated = false, foreignPowerRelated = false)
    every { aggravatingFactorRepository.findByCodeIn(emptyList()) } returns emptyList()

    service.replaceAggravatingFactors(charge)

    assertThat(charge.chargeAggravatingFactors).isEmpty()
  }

  @Test
  fun `does not add any factors when both flags are null`() {
    val charge = createCharge(terrorRelated = null, foreignPowerRelated = null)
    every { aggravatingFactorRepository.findByCodeIn(emptyList()) } returns emptyList()

    service.replaceAggravatingFactors(charge)

    assertThat(charge.chargeAggravatingFactors).isEmpty()
  }

  @Test
  fun `replaces previously set aggravating factors when flags change`() {
    val charge = createCharge(terrorRelated = true, foreignPowerRelated = false)
    val terrorFactor = createAggravatingFactorEntity(1, "OATC")
    val existingFactor = createAggravatingFactorEntity(2, "OAFPC")
    // Pre-populate a stale factor to simulate a prior save
    charge.chargeAggravatingFactors.add(ChargeAggravatingFactorEntity(charge, existingFactor))

    every { aggravatingFactorRepository.findByCodeIn(listOf("OATC")) } returns listOf(terrorFactor)

    service.replaceAggravatingFactors(charge)

    assertThat(charge.chargeAggravatingFactors).hasSize(1)
    assertThat(charge.chargeAggravatingFactors.first().aggravatingFactor.code).isEqualTo("OATC")
  }

  @Test
  fun `does not clear or flush when aggravating factors are unchanged`() {
    val charge = createCharge(terrorRelated = true)
    val terrorFactor = createAggravatingFactorEntity(1, "OATC")
    charge.chargeAggravatingFactors.add(ChargeAggravatingFactorEntity(charge, terrorFactor))

    service.replaceAggravatingFactors(charge)

    verify(exactly = 0) { chargeRepository.saveAndFlush(any()) }
    verify(exactly = 0) { aggravatingFactorRepository.findByCodeIn(any()) }
    assertThat(charge.chargeAggravatingFactors).hasSize(1)
  }

  private fun createCharge(
    terrorRelated: Boolean? = null,
    foreignPowerRelated: Boolean? = null,
  ): ChargeEntity = ChargeEntity(
    id = 1,
    chargeUuid = UUID.randomUUID(),
    offenceCode = "OFF001",
    offenceStartDate = null,
    offenceEndDate = null,
    statusId = ChargeEntityStatus.ACTIVE,
    chargeOutcome = null,
    supersedingCharge = null,
    terrorRelated = terrorRelated,
    foreignPowerRelated = foreignPowerRelated,
    domesticViolenceRelated = null,
    createdAt = ZonedDateTime.now(),
    createdBy = "test-user",
    createdPrison = null,
  )

  private fun createAggravatingFactorEntity(id: Int, code: String) = AggravatingFactorEntity(
    id = id,
    code = code,
    title = "Title for $code",
    status = AggravatingFactorStatus.ACTIVE,
    displayOrder = id,
  )
}
