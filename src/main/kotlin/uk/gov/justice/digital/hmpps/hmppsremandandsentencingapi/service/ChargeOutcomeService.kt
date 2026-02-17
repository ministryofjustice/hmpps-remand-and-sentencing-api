package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import org.springframework.core.MethodParameter
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.ChargeOutcome
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.chargeoutcome.CreateChargeOutcome
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.UpdatedChargeOutcome
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeOutcomeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ReferenceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.ChargeOutcomeRepository
import java.util.*

@Service
class ChargeOutcomeService(private val chargeOutcomeRepository: ChargeOutcomeRepository) {

  @Transactional
  fun createChargeOutcome(createChargeOutcome: CreateChargeOutcome): ChargeOutcomeEntity {
    val bindingResults = BeanPropertyBindingResult(createChargeOutcome, "createChargeOutcome")
    validateChargeOutcome(createChargeOutcome, bindingResults)

    val chargeOutcomeFromNomisCode = chargeOutcomeRepository.findByNomisCode(createChargeOutcome.nomisCode)
    if (chargeOutcomeFromNomisCode != null) {
      bindingResults.addError(
        FieldError("createChargeOutcome", "nomisCode", "nomisCode outcome code is already mapped"),
      )
    }

    if (bindingResults.hasErrors()) {
      throw MethodArgumentNotValidException(
        MethodParameter(this.javaClass.getDeclaredMethod("createChargeOutcome", CreateChargeOutcome::class.java), 0),
        bindingResults,
      )
    }

    return chargeOutcomeRepository.save(ChargeOutcomeEntity.from(createChargeOutcome))
  }

  @Transactional
  fun updateChargeOutcome(chargeOutcomeUuid: UUID, updateChargeOutcome: CreateChargeOutcome): UpdatedChargeOutcome {
    val bindingResults = BeanPropertyBindingResult(updateChargeOutcome, "createChargeOutcome")
    validateChargeOutcome(updateChargeOutcome, bindingResults)

    val chargeOutcomeFromNomisCode = chargeOutcomeRepository.findByNomisCode(updateChargeOutcome.nomisCode)
    if (chargeOutcomeFromNomisCode != null && chargeOutcomeFromNomisCode.outcomeUuid != chargeOutcomeUuid) {
      bindingResults.addError(
        FieldError("createChargeOutcome", "nomisCode", "nomisCode outcome code is already mapped"),
      )
    }

    if (bindingResults.hasErrors()) {
      throw MethodArgumentNotValidException(
        MethodParameter(this.javaClass.getDeclaredMethod("createChargeOutcome", CreateChargeOutcome::class.java), 0),
        bindingResults,
      )
    }

    val (existingChargeOutcome, isNew) = chargeOutcomeRepository.findByOutcomeUuid(chargeOutcomeUuid)
      ?.let { it to false } ?: (
      chargeOutcomeRepository.save(
        ChargeOutcomeEntity.from(updateChargeOutcome.copy(outcomeUuid = chargeOutcomeUuid)),
      ) to true
      )

    val migrateNomisCodeData = isNew || existingChargeOutcome.nomisCode != updateChargeOutcome.nomisCode
    existingChargeOutcome.updateFrom(chargeOutcomeUuid, updateChargeOutcome)
    return UpdatedChargeOutcome(existingChargeOutcome, migrateNomisCodeData)
  }

  private fun validateChargeOutcome(
    createChargeOutcome: CreateChargeOutcome,
    bindingResults: BeanPropertyBindingResult,
  ) {
    val outcomeTypes = chargeOutcomeRepository.findDistinctOutcomeTypes()
    if (!outcomeTypes.contains(createChargeOutcome.outcomeType)) {
      bindingResults.addError(
        FieldError(
          "createChargeOutcome",
          "outcomeType",
          "Must use one of existing the outcome types ${outcomeTypes.sorted().joinToString()}",
        ),
      )
    }
    val dispositionCodes = chargeOutcomeRepository.findDistinctDispositionCodes()
    if (!dispositionCodes.contains(createChargeOutcome.dispositionCode)) {
      bindingResults.addError(
        FieldError(
          "createChargeOutcome",
          "dispositionCode",
          "Must use one of existing the disposition codes ${dispositionCodes.sorted().joinToString()}",
        ),
      )
    }
  }

  fun getAllByStatus(statuses: List<ReferenceEntityStatus>): List<ChargeOutcome> = chargeOutcomeRepository.findByStatusIn(statuses).map { ChargeOutcome.from(it) }

  fun findByUuid(outcomeUuid: UUID): ChargeOutcome? = chargeOutcomeRepository.findByOutcomeUuid(outcomeUuid)?.let { ChargeOutcome.from(it) }

  fun findByUuids(outcomeUuids: List<UUID>): List<ChargeOutcome> = chargeOutcomeRepository.findByOutcomeUuidIn(outcomeUuids).map { ChargeOutcome.from(it) }
}
