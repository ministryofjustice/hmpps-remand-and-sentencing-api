package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import org.springframework.core.MethodParameter
import org.springframework.stereotype.Service
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.ChargeOutcome
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.chargeoutcome.CreateChargeOutcome
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeOutcomeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ReferenceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.ChargeOutcomeRepository
import java.util.UUID

@Service
class ChargeOutcomeService(private val chargeOutcomeRepository: ChargeOutcomeRepository) {

  fun createChargeOutcome(createChargeOutcome: CreateChargeOutcome): ChargeOutcome {
    val outcomeTypes = chargeOutcomeRepository.findDistinctOutcomeTypes()
    val bindingResults = BeanPropertyBindingResult(createChargeOutcome, "createChargeOutcome")
    if (!outcomeTypes.contains(createChargeOutcome.outcomeType)) {
      bindingResults.addError(FieldError("createChargeOutcome", "outcomeType", "Must use one of existing the outcome types ${outcomeTypes.joinToString()}"))
    }
    val dispositionCodes = chargeOutcomeRepository.findDistinctDispositionCodes()
    if (!dispositionCodes.contains(createChargeOutcome.dispositionCode)) {
      bindingResults.addError(
        FieldError("createChargeOutcome", "dispositionCode", "Must use one of existing the disposition codes ${dispositionCodes.joinToString()}"),
      )
    }

    if (bindingResults.hasErrors()) {
      throw MethodArgumentNotValidException(
        MethodParameter(this.javaClass.getDeclaredMethod("createChargeOutcome", CreateChargeOutcome::class.java), 0),
        bindingResults,
      )
    }

    val chargeOutcome = chargeOutcomeRepository.save(ChargeOutcomeEntity.from(createChargeOutcome))
    return ChargeOutcome.from(chargeOutcome)
  }

  fun getAllByStatus(statuses: List<ReferenceEntityStatus>): List<ChargeOutcome> = chargeOutcomeRepository.findByStatusIn(statuses).map { ChargeOutcome.from(it) }

  fun findByUuid(outcomeUuid: UUID): ChargeOutcome? = chargeOutcomeRepository.findByOutcomeUuid(outcomeUuid)?.let { ChargeOutcome.from(it) }

  fun findByUuids(outcomeUuids: List<UUID>): List<ChargeOutcome> = chargeOutcomeRepository.findByOutcomeUuidIn(outcomeUuids).map { ChargeOutcome.from(it) }
}
