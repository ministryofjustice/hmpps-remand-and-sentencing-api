package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
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
  fun `should add OATC aggravating factor when OATC code is provided`() {
    val charge = createCharge(terrorRelated = true)
    val terrorFactor = createAggravatingFactorEntity(1, "OATC")
    every { aggravatingFactorRepository.findByCodeIn(listOf("OATC")) } returns listOf(terrorFactor)

    aggravatingFactorsService.replaceAggravatingFactors(charge, setOf("OATC"))

    assertThat(charge.chargeAggravatingFactors).hasSize(1)
    assertThat(charge.chargeAggravatingFactors.first().aggravatingFactor.code).isEqualTo("OATC")
  }

  @Test
  fun `should add OAFPC aggravating factor when OAFPC code is provided`() {
    val charge = createCharge(foreignPowerRelated = true)
    val foreignPowerFactor = createAggravatingFactorEntity(2, "OAFPC")
    every { aggravatingFactorRepository.findByCodeIn(listOf("OAFPC")) } returns listOf(foreignPowerFactor)

    aggravatingFactorsService.replaceAggravatingFactors(charge, setOf("OAFPC"))

    assertThat(charge.chargeAggravatingFactors).hasSize(1)
    assertThat(charge.chargeAggravatingFactors.first().aggravatingFactor.code).isEqualTo("OAFPC")
  }

  @Test
  fun `should add both OATC and OAFPC when both codes are provided`() {
    val charge = createCharge(terrorRelated = true, foreignPowerRelated = true)
    val terrorFactor = createAggravatingFactorEntity(1, "OATC")
    val foreignPowerFactor = createAggravatingFactorEntity(2, "OAFPC")
    val codesSlot = slot<List<String>>()
    every { aggravatingFactorRepository.findByCodeIn(capture(codesSlot)) } returns listOf(terrorFactor, foreignPowerFactor)

    aggravatingFactorsService.replaceAggravatingFactors(charge, setOf("OATC", "OAFPC"))

    assertThat(codesSlot.captured).containsExactlyInAnyOrder("OATC", "OAFPC")
    assertThat(charge.chargeAggravatingFactors).hasSize(2)
    assertThat(charge.chargeAggravatingFactors.map { it.aggravatingFactor.code })
      .containsExactlyInAnyOrder("OATC", "OAFPC")
  }

  @Test
  fun `should not add any factors when empty codes set is provided`() {
    val charge = createCharge()

    aggravatingFactorsService.replaceAggravatingFactors(charge, emptySet())

    assertThat(charge.chargeAggravatingFactors).isEmpty()
  }

  @Test
  fun `should replace previously set aggravating factors when codes change`() {
    val charge = createCharge(terrorRelated = true)
    val terrorFactor = createAggravatingFactorEntity(1, "OATC")
    val existingFactor = createAggravatingFactorEntity(2, "OAFPC")
    charge.chargeAggravatingFactors.add(ChargeAggravatingFactorEntity(charge, existingFactor))
    every { aggravatingFactorRepository.findByCodeIn(listOf("OATC")) } returns listOf(terrorFactor)

    aggravatingFactorsService.replaceAggravatingFactors(charge, setOf("OATC"))

    assertThat(charge.chargeAggravatingFactors).hasSize(1)
    assertThat(charge.chargeAggravatingFactors.first().aggravatingFactor.code).isEqualTo("OATC")
  }

  @Test
  fun `should not re-fetch when aggravating factors are unchanged`() {
    val charge = createCharge(terrorRelated = true)
    val terrorFactor = createAggravatingFactorEntity(1, "OATC")
    charge.chargeAggravatingFactors.add(ChargeAggravatingFactorEntity(charge, terrorFactor))

    aggravatingFactorsService.replaceAggravatingFactors(charge, setOf("OATC"))

    verify(exactly = 0) { aggravatingFactorRepository.findByCodeIn(any()) }
    assertThat(charge.chargeAggravatingFactors).hasSize(1)
  }

  @Test
  fun `should remove aggravating factors referencing a previous charge`() {
    val previousCharge = createCharge(terrorRelated = true)
    val currentCharge = createCharge(terrorRelated = true)
    val terrorFactor = createAggravatingFactorEntity(1, "OATC")
    currentCharge.chargeAggravatingFactors.add(ChargeAggravatingFactorEntity(previousCharge, terrorFactor))
    every { aggravatingFactorRepository.findByCodeIn(listOf("OATC")) } returns listOf(terrorFactor)

    aggravatingFactorsService.replaceAggravatingFactors(currentCharge, setOf("OATC"))

    assertThat(currentCharge.chargeAggravatingFactors).hasSize(1)
    assertThat(currentCharge.chargeAggravatingFactors.first().charge).isEqualTo(currentCharge)
    assertThat(currentCharge.chargeAggravatingFactors.first().aggravatingFactor.code).isEqualTo("OATC")
  }

  @Test
  fun `should throw when terrorRelated is true but OATC is absent from non-empty codes`() {
    val charge = createCharge(terrorRelated = true)

    assertThatThrownBy {
      aggravatingFactorsService.replaceAggravatingFactors(charge, setOf("OAFPC"))
    }.isInstanceOf(RuntimeException::class.java)
      .hasMessageContaining("mis-match")
  }

  @Test
  fun `should throw when foreignPowerRelated is true but OAFPC is absent from non-empty codes`() {
    val charge = createCharge(foreignPowerRelated = true)

    assertThatThrownBy {
      aggravatingFactorsService.replaceAggravatingFactors(charge, setOf("OATC"))
    }.isInstanceOf(RuntimeException::class.java)
      .hasMessageContaining("mis-match")
  }

  @Test
  fun `should throw when OATC is in codes but terrorRelated is not true`() {
    val charge = createCharge(terrorRelated = null)

    assertThatThrownBy {
      aggravatingFactorsService.replaceAggravatingFactors(charge, setOf("OATC"))
    }.isInstanceOf(RuntimeException::class.java)
      .hasMessageContaining("mis-match")
  }

  @Test
  fun `should throw when OAFPC is in codes but foreignPowerRelated is not true`() {
    val charge = createCharge(foreignPowerRelated = null)

    assertThatThrownBy {
      aggravatingFactorsService.replaceAggravatingFactors(charge, setOf("OAFPC"))
    }.isInstanceOf(RuntimeException::class.java)
      .hasMessageContaining("mis-match")
  }

  @Test
  fun `should fall back to charge flags when codes are empty and terrorRelated is true`() {
    val charge = createCharge(terrorRelated = true)
    val terrorFactor = createAggravatingFactorEntity(1, "OATC")
    every { aggravatingFactorRepository.findByCodeIn(listOf("OATC")) } returns listOf(terrorFactor)

    aggravatingFactorsService.replaceAggravatingFactors(charge, emptySet())

    assertThat(charge.chargeAggravatingFactors).hasSize(1)
    assertThat(charge.chargeAggravatingFactors.first().aggravatingFactor.code).isEqualTo("OATC")
  }

  @Test
  fun `should fall back to charge flags when codes are empty and foreignPowerRelated is true`() {
    val charge = createCharge(foreignPowerRelated = true)
    val foreignPowerFactor = createAggravatingFactorEntity(2, "OAFPC")
    every { aggravatingFactorRepository.findByCodeIn(listOf("OAFPC")) } returns listOf(foreignPowerFactor)

    aggravatingFactorsService.replaceAggravatingFactors(charge, emptySet())

    assertThat(charge.chargeAggravatingFactors).hasSize(1)
    assertThat(charge.chargeAggravatingFactors.first().aggravatingFactor.code).isEqualTo("OAFPC")
  }

  @Test
  fun `should fall back to charge flags when codes are empty and both flags are true`() {
    val charge = createCharge(terrorRelated = true, foreignPowerRelated = true)
    val terrorFactor = createAggravatingFactorEntity(1, "OATC")
    val foreignPowerFactor = createAggravatingFactorEntity(2, "OAFPC")
    val codesSlot = slot<List<String>>()
    every { aggravatingFactorRepository.findByCodeIn(capture(codesSlot)) } returns listOf(terrorFactor, foreignPowerFactor)

    aggravatingFactorsService.replaceAggravatingFactors(charge, emptySet())

    assertThat(codesSlot.captured).containsExactlyInAnyOrder("OATC", "OAFPC")
    assertThat(charge.chargeAggravatingFactors).hasSize(2)
    assertThat(charge.chargeAggravatingFactors.map { it.aggravatingFactor.code })
      .containsExactlyInAnyOrder("OATC", "OAFPC")
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
  fun `should add non-flag-backed codes alongside flag-backed codes`() {
    val charge = createCharge(terrorRelated = true)
    val terrorFactor = createAggravatingFactorEntity(1, "OATC")
    val tggFactor = createAggravatingFactorEntity(3, "TGG")
    val codesSlot = slot<List<String>>()
    every { aggravatingFactorRepository.findByCodeIn(capture(codesSlot)) } returns listOf(terrorFactor, tggFactor)

    aggravatingFactorsService.replaceAggravatingFactors(charge, setOf("OATC", "TGG"))

    assertThat(codesSlot.captured).containsExactlyInAnyOrder("OATC", "TGG")
    assertThat(charge.chargeAggravatingFactors.map { it.aggravatingFactor.code })
      .containsExactlyInAnyOrder("OATC", "TGG")
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
