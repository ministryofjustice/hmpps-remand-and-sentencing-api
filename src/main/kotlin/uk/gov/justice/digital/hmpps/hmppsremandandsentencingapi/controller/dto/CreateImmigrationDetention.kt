package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ImmigrationDetentionNoLongerOfInterestType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ImmigrationDetentionRecordType
import java.time.LocalDate
import java.util.UUID

data class CreateImmigrationDetention(
  val prisonerId: String,
  val appearanceOutcomeUuid: UUID,
  var immigrationDetentionRecordType: ImmigrationDetentionRecordType,
  var recordDate: LocalDate,
  @field:Size(min = 5, max = 16, message = "The Home Office Reference Number should be between 5 and 16 characters.")
  @field:Pattern(regexp = "^[A-Za-z0-9/]+$", message = "The Home Office Reference Number should contain only letters, numbers and '/'")
  var homeOfficeReferenceNumber: String? = null,
  var noLongerOfInterestReason: ImmigrationDetentionNoLongerOfInterestType? = null,
  var noLongerOfInterestComment: String? = null,
  val createdByUsername: String,
  val createdByPrison: String,
  val courtAppearanceUuid: UUID?,
)
