package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ImmigrationDetentionNoLongerOfInterestType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ImmigrationDetentionRecordType
import java.time.LocalDate

data class CreateImmigrationDetention(
  val prisonerId: String,
  var immigrationDetentionRecordType: ImmigrationDetentionRecordType,
  var recordDate: LocalDate,
  var homeOfficeReferenceNumber: String? = null,
  var noLongerOfInterestReason: ImmigrationDetentionNoLongerOfInterestType? = null,
  var noLongerOfInterestComment: String? = null,
  val createdByUsername: String,
  val createdByPrison: String,
)
