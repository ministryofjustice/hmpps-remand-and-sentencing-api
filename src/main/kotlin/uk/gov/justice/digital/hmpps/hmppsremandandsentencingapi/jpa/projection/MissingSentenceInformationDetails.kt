package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.projection

import java.time.LocalDate
import java.util.UUID

data class MissingSentenceInformationDetails(
  val appearanceUuid: UUID,
  val appearanceDate: LocalDate,
  val courtCode: String,
  val courtCaseReference: String?,
  val sentenceUuid: UUID,
  val chargeUuid: UUID,
  val offenceCode: String,
  val offenceStartDate: LocalDate?,
  val countNumber: String?,
  val convictionDate: LocalDate?,
  val sentenceServeType: String,
  val sentenceTypeId: Int,
  val sentenceTypeDescription: String,
  val periodLengthUuid: UUID?,
  val years: Int?,
  val months: Int?,
  val weeks: Int?,
  val days: Int?,
  val periodOrder: String?,
  val periodLengthType: String?,
)
