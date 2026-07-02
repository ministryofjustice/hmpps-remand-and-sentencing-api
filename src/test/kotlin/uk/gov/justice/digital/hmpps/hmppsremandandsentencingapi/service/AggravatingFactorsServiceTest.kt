package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.AggravatingFactorEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeAggravatingFactorEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.AggravatingFactorStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ChargeEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.AggravatingFactorRepository
import java.time.ZonedDateTime
import java.util.UUID

class AggravatingFactorsServiceTest {

  private val aggravatingFactorRepository = mockk<AggravatingFactorRepository>(relaxed = true)
  private val aggravatingFactorsService = AggravatingFactorsService(
    aggravatingFactorRepository = aggravatingFactorRepository,
  )

  @Test
  fun `should not add any factors when empty codes set is provided`() {
    val charge = createCharge()

    aggravatingFactorsService.replaceAggravatingFactors(charge, emptySet())

    assertThat(charge.chargeAggravatingFactors).isEmpty()
  }

  @Test
  fun `should add a non-flag-backed code`() {
    val charge = createCharge()
    val tggFactor = createAggravatingFactorEntity(3, "TGG")
    every { aggravatingFactorRepository.findByCodeIn(listOf("TGG")) } returns listOf(tggFactor)

    aggravatingFactorsService.replaceAggravatingFactors(charge, setOf("TGG"))

    assertThat(charge.chargeAggravatingFactors).hasSize(1)
    assertThat(charge.chargeAggravatingFactors.first().aggravatingFactor.code).isEqualTo("TGG")
  }

  @Test
  fun `should add multiple non-flag-backed codes`() {
    val charge = createCharge()
    val tggFactor = createAggravatingFactorEntity(3, "TGG")
    val tgvFactor = createAggravatingFactorEntity(4, "TGV")
    val ewaFactor = createAggravatingFactorEntity(5, "EWA")
    val codesSlot = slot<List<String>>()
    every { aggravatingFactorRepository.findByCodeIn(capture(codesSlot)) } returns listOf(tggFactor, tgvFactor, ewaFactor)

    aggravatingFactorsService.replaceAggravatingFactors(charge, setOf("TGG", "TGV", "EWA"))

    assertThat(codesSlot.captured).containsExactlyInAnyOrder("TGG", "TGV", "EWA")
    assertThat(charge.chargeAggravatingFactors.map { it.aggravatingFactor.code })
      .containsExactlyInAnyOrder("TGG", "TGV", "EWA")
  }

  @Test
  fun `should not re-fetch unchanged non-flag-backed codes`() {
    val charge = createCharge()
    val tggFactor = createAggravatingFactorEntity(3, "TGG")
    charge.chargeAggravatingFactors.add(ChargeAggravatingFactorEntity(charge, tggFactor))

    aggravatingFactorsService.replaceAggravatingFactors(charge, setOf("TGG"))

    verify(exactly = 0) { aggravatingFactorRepository.findByCodeIn(any()) }
    assertThat(charge.chargeAggravatingFactors).hasSize(1)
    assertThat(charge.chargeAggravatingFactors.first().aggravatingFactor.code).isEqualTo("TGG")
  }

  @Test
  fun `should remove non-flag-backed codes no longer in the codes set`() {
    val charge = createCharge()
    val tggFactor = createAggravatingFactorEntity(3, "TGG")
    val tgvFactor = createAggravatingFactorEntity(4, "TGV")
    charge.chargeAggravatingFactors.add(ChargeAggravatingFactorEntity(charge, tggFactor))
    charge.chargeAggravatingFactors.add(ChargeAggravatingFactorEntity(charge, tgvFactor))
    every { aggravatingFactorRepository.findByCodeIn(listOf("TGG")) } returns listOf(tggFactor)

    aggravatingFactorsService.replaceAggravatingFactors(charge, setOf("TGG"))

    assertThat(charge.chargeAggravatingFactors).hasSize(1)
    assertThat(charge.chargeAggravatingFactors.first().aggravatingFactor.code).isEqualTo("TGG")
  }

  private fun createCharge(): ChargeEntity = ChargeEntity(
    id = 1,
    chargeUuid = UUID.randomUUID(),
    offenceCode = "OFF001",
    offenceStartDate = null,
    offenceEndDate = null,
    statusId = ChargeEntityStatus.ACTIVE,
    chargeOutcome = null,
    supersedingCharge = null,
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
