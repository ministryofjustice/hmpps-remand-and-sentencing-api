package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import org.springframework.core.MethodParameter
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CourtAppearanceOutcome
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.appearanceoutcome.CreateAppearanceOutcome
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.UpdatedAppearanceOutcome
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.AppearanceOutcomeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ReferenceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.AppearanceOutcomeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.ChargeOutcomeRepository
import java.util.UUID

@Service
class AppearanceOutcomeService(private val appearanceOutcomeRepository: AppearanceOutcomeRepository, private val chargeOutcomeRepository: ChargeOutcomeRepository) {

  @Transactional
  fun createAppearanceOutcome(createAppearanceOutcome: CreateAppearanceOutcome): AppearanceOutcomeEntity {
    val bindingResults = BeanPropertyBindingResult(createAppearanceOutcome, "createAppearanceOutcome")
    validateAppearanceOutcome(createAppearanceOutcome, bindingResults)

    val appearanceOutcomeFromNomisCode = appearanceOutcomeRepository.findByNomisCode(createAppearanceOutcome.nomisCode)
    if (appearanceOutcomeFromNomisCode != null) {
      bindingResults.addError(
        FieldError("createAppearanceOutcome", "nomisCode", "nomisCode outcome code is already mapped"),
      )
    }

    if (bindingResults.hasErrors()) {
      throw MethodArgumentNotValidException(
        MethodParameter(this.javaClass.getDeclaredMethod("createAppearanceOutcome", CreateAppearanceOutcome::class.java), 0),
        bindingResults,
      )
    }

    return appearanceOutcomeRepository.save(AppearanceOutcomeEntity.from(createAppearanceOutcome))
  }

  @Transactional
  fun updateAppearanceOutcome(appearanceOutcomeUuid: UUID, updateAppearanceOutcome: CreateAppearanceOutcome): UpdatedAppearanceOutcome {
    val bindingResults = BeanPropertyBindingResult(updateAppearanceOutcome, "updateAppearanceOutcome")
    validateAppearanceOutcome(updateAppearanceOutcome, bindingResults)

    val appearanceOutcomeFromNomisCode = appearanceOutcomeRepository.findByNomisCode(updateAppearanceOutcome.nomisCode)
    if (appearanceOutcomeFromNomisCode != null && appearanceOutcomeFromNomisCode.outcomeUuid != appearanceOutcomeUuid) {
      bindingResults.addError(
        FieldError("updateAppearanceOutcome", "nomisCode", "nomisCode outcome code is already mapped"),
      )
    }
    if (bindingResults.hasErrors()) {
      throw MethodArgumentNotValidException(
        MethodParameter(this.javaClass.getDeclaredMethod("updateAppearanceOutcome", UUID::class.java, CreateAppearanceOutcome::class.java), 1),
        bindingResults,
      )
    }

    val (existingAppearanceOutcome, isNew) = appearanceOutcomeRepository.findByOutcomeUuid(appearanceOutcomeUuid)
      ?.let { it to false } ?: (
      appearanceOutcomeRepository.save(
        AppearanceOutcomeEntity.from(updateAppearanceOutcome.copy(outcomeUuid = appearanceOutcomeUuid)),
      ) to true
      )
    val migrateNomisCodeData = isNew || existingAppearanceOutcome.nomisCode != updateAppearanceOutcome.nomisCode
    existingAppearanceOutcome.updateFrom(appearanceOutcomeUuid, updateAppearanceOutcome)
    return UpdatedAppearanceOutcome(existingAppearanceOutcome, migrateNomisCodeData)
  }

  private fun validateAppearanceOutcome(
    createAppearanceOutcome: CreateAppearanceOutcome,
    bindingResults: BeanPropertyBindingResult,
  ) {
    val outcomeTypes = appearanceOutcomeRepository.findDistinctOutcomeTypes()
    if (!outcomeTypes.contains(createAppearanceOutcome.outcomeType)) {
      bindingResults.addError(
        FieldError(
          "createAppearanceOutcome",
          "outcomeType",
          "Must use one of existing the outcome types ${outcomeTypes.sorted().joinToString()}",
        ),
      )
    }
    val dispositionCodes = appearanceOutcomeRepository.findDistinctDispositionCodes()
    if (!dispositionCodes.contains(createAppearanceOutcome.dispositionCode)) {
      bindingResults.addError(
        FieldError(
          "createAppearanceOutcome",
          "dispositionCode",
          "Must use one of existing the disposition codes ${dispositionCodes.sorted().joinToString()}",
        ),
      )
    }

    val warrantTypes = appearanceOutcomeRepository.findDistinctWarrantTypes()
    if (!warrantTypes.contains(createAppearanceOutcome.warrantType)) {
      bindingResults.addError(
        FieldError(
          "createAppearanceOutcome",
          "warrantType",
          "Must use one of existing the warrant types ${warrantTypes.sorted().joinToString()}",
        ),
      )
    }
    val relatedChargeOutcome = chargeOutcomeRepository.findByOutcomeUuid(createAppearanceOutcome.relatedChargeOutcomeUuid)
    if (relatedChargeOutcome == null) {
      bindingResults.addError(
        FieldError(
          "createAppearanceOutcome",
          "relatedChargeOutcomeUuid",
          "Must supply a related charge outcome uuid which exists in the charge outcome table",
        ),
      )
    }
  }

  fun getAllByStatus(statuses: List<ReferenceEntityStatus>): List<CourtAppearanceOutcome> = appearanceOutcomeRepository.findByStatusIn(statuses).map { CourtAppearanceOutcome.from(it) }

  fun findByUuid(outcomeUuid: UUID): CourtAppearanceOutcome? = appearanceOutcomeRepository.findByOutcomeUuid(outcomeUuid)?.let { CourtAppearanceOutcome.from(it) }
}
