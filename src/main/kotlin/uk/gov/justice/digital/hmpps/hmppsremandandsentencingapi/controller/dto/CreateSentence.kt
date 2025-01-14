package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.Nulls
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.util.UUID

data class CreateSentence(
  val sentenceUuid: UUID?,
  @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonSetter(nulls = Nulls.SKIP)
  var lifetimeSentenceUuid: UUID = UUID.randomUUID(),
  val chargeNumber: String?,
  val periodLengths: List<CreatePeriodLength>,
  val sentenceServeType: String,
  val consecutiveToChargeNumber: String?,
  val consecutiveToSentenceUuid: UUID?,
  val sentenceTypeId: UUID,
  val convictionDate: LocalDate?,
  val fineAmount: CreateFineAmount?,
  val prisonId: String?,
)
