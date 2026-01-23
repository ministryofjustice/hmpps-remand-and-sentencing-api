package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ImmigrationDetentionEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ImmigrationDetentionRecordType

class ImmigrationDetentionEntityUpdater {

  companion object {
    fun update(existingEntity: ImmigrationDetentionEntity, updatedValues: ImmigrationDetentionEntity) {
      existingEntity.prisonerId = updatedValues.prisonerId
      existingEntity.updatedAt = updatedValues.createdAt
      existingEntity.updatedBy = updatedValues.createdByUsername
      existingEntity.updatedPrison = updatedValues.updatedPrison ?: updatedValues.createdPrison
      existingEntity.recordDate = updatedValues.recordDate
      if (existingEntity.immigrationDetentionRecordType != updatedValues.immigrationDetentionRecordType) {
        when (existingEntity.immigrationDetentionRecordType) {
          ImmigrationDetentionRecordType.IS91 -> {
            if (updatedValues.immigrationDetentionRecordType != ImmigrationDetentionRecordType.DEPORTATION_ORDER) {
              existingEntity.homeOfficeReferenceNumber = null
            }
          }
          ImmigrationDetentionRecordType.DEPORTATION_ORDER -> {
            if (updatedValues.immigrationDetentionRecordType != ImmigrationDetentionRecordType.IS91) {
              existingEntity.homeOfficeReferenceNumber = null
            }
          }
          ImmigrationDetentionRecordType.IMMIGRATION_BAIL -> {
            existingEntity.homeOfficeReferenceNumber = null
          }
          ImmigrationDetentionRecordType.NO_LONGER_OF_INTEREST -> {
            existingEntity.noLongerOfInterestReason = null
            existingEntity.noLongerOfInterestComment = null
          }
          else -> {
            existingEntity.homeOfficeReferenceNumber = null
            existingEntity.noLongerOfInterestReason = null
            existingEntity.noLongerOfInterestComment = null
          }
        }
        existingEntity.immigrationDetentionRecordType = updatedValues.immigrationDetentionRecordType
      }
    }
  }
}
