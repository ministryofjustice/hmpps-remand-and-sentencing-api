package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.core.MethodParameter
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.hmctsresultcode.CreateHmctsResultCode
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.hmctsresultcode.HmctsResultCode
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeOutcomeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.HmctsResultCodeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ReferenceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.ChargeOutcomeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.HmctsResultCodeRepository
import java.util.*

@Service
class HmctsResultCodeService(private val hmctsResultCodeRepository: HmctsResultCodeRepository, private val chargeOutcomeRepository: ChargeOutcomeRepository) {

  @Transactional
  fun createHmctsResultCode(createHmctsResultCode: CreateHmctsResultCode): HmctsResultCode {
    val chargeOutcome = chargeOutcomeRepository.findByOutcomeUuid(createHmctsResultCode.outcomeUuid)
    val bindingResults = BeanPropertyBindingResult(createHmctsResultCode, "createHmctsResultCode")
    validateHmctsResultCode(createHmctsResultCode, chargeOutcome, bindingResults)

    if (bindingResults.hasErrors()) {
      throw MethodArgumentNotValidException(
        MethodParameter(this.javaClass.getDeclaredMethod("createHmctsResultCode", CreateHmctsResultCode::class.java), 0),
        bindingResults,
      )
    }

    return HmctsResultCode.from(hmctsResultCodeRepository.save(HmctsResultCodeEntity.from(createHmctsResultCode, chargeOutcome!!)))
  }

  @Transactional
  fun updateHmctsResultCode(resultUuid: UUID, updateHmctsResultCode: CreateHmctsResultCode): HmctsResultCode {
    val result = hmctsResultCodeRepository.findByResultUuid(resultUuid) ?: throw EntityNotFoundException("HMCTS ResultCode not found for $resultUuid")
    val chargeOutcome = chargeOutcomeRepository.findByOutcomeUuid(updateHmctsResultCode.outcomeUuid)
    val bindingResults = BeanPropertyBindingResult(updateHmctsResultCode, "createHmctsResultCode")
    validateHmctsResultCode(updateHmctsResultCode, chargeOutcome, bindingResults)

    if (bindingResults.hasErrors()) {
      throw MethodArgumentNotValidException(
        MethodParameter(this.javaClass.getDeclaredMethod("createHmctsResultCode", CreateHmctsResultCode::class.java), 0),
        bindingResults,
      )
    }

    result.updateFrom(resultUuid, updateHmctsResultCode, chargeOutcome!!)
    return HmctsResultCode.from(result)
  }

  private fun validateHmctsResultCode(
    createHmctsResultCode: CreateHmctsResultCode,
    chargeOutcome: ChargeOutcomeEntity?,
    bindingResults: BeanPropertyBindingResult,
  ) {
    val existingResultCode = hmctsResultCodeRepository.findByResultCode(createHmctsResultCode.resultCode)
    if (existingResultCode != null && existingResultCode.resultUuid != createHmctsResultCode.resultUuid) {
      bindingResults.addError(
        FieldError("createHmctsResultCode", "resultCode", "resultCode HMCTS result code is already mapped"),
      )
    }

    if (chargeOutcome == null) {
      bindingResults.addError(
        FieldError("createHmctsResultCode", "outcomeUuid", "outcomeUuid charge outcome not found"),
      )
    }
  }

  fun getAllByStatus(statuses: List<ReferenceEntityStatus>): List<HmctsResultCode> = hmctsResultCodeRepository.findByStatusIn(statuses).map { HmctsResultCode.from(it) }

  fun findByUuid(outcomeUuid: UUID): HmctsResultCode? = hmctsResultCodeRepository.findByResultUuid(outcomeUuid)?.let { HmctsResultCode.from(it) }

  fun findByUuids(outcomeUuids: List<UUID>): List<HmctsResultCode> = hmctsResultCodeRepository.findByResultUuidIn(outcomeUuids).map { HmctsResultCode.from(it) }
}
