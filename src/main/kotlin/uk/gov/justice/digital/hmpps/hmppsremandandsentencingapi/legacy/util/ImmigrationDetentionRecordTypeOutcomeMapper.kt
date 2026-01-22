package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.util

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.AppearanceOutcomeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ImmigrationDetentionRecordType
import java.util.UUID

class ImmigrationDetentionRecordTypeOutcomeMapper {
  companion object {
    fun outcomeToRecordType(appearanceOutcome: AppearanceOutcomeEntity?): ImmigrationDetentionRecordType = when (appearanceOutcome?.outcomeUuid) {
      UUID.fromString("5c670576-ffbf-4005-8d54-4aeba7bf1a22") -> ImmigrationDetentionRecordType.IS91
      UUID.fromString("d774d9dd-12e8-4b6e-88e8-3e7739dff9e1") -> ImmigrationDetentionRecordType.IMMIGRATION_BAIL
      UUID.fromString("b28afb19-dd94-4970-8071-e616b33274cb") -> ImmigrationDetentionRecordType.DEPORTATION_ORDER
      UUID.fromString("15524814-3238-4e4b-86a7-cda31b0221ec") -> ImmigrationDetentionRecordType.NO_LONGER_OF_INTEREST
      else -> ImmigrationDetentionRecordType.UNKNOWN
    }
  }
}
