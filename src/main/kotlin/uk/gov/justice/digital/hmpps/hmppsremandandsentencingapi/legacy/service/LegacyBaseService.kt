package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.service

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.AppearanceChargeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.AppearanceChargeHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.ChargeHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.ChargeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.AppearanceChargeHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.ChargeHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.ServiceUserService

abstract class LegacyBaseService(
  protected val chargeRepository: ChargeRepository,
  protected val appearanceChargeHistoryRepository: AppearanceChargeHistoryRepository,
  protected val chargeHistoryRepository: ChargeHistoryRepository,
  protected val serviceUserService: ServiceUserService,
) {

  fun createChargeRecordIfOverManyAppearancesOrUpdate(existingCharge: ChargeEntity, appearance: CourtAppearanceEntity, updatedCharge: ChargeEntity, performedByUsername: String, chargeModifyFunction: (ChargeEntity) -> Unit = {}): ChargeEntity {
    var chargeRecord = existingCharge
    if (existingCharge.hasTwoOrMoreActiveCourtAppearance(appearance)) {
      existingCharge.appearanceCharges.firstOrNull { it.appearance == appearance }
        ?.let { appearanceCharge ->
          appearanceChargeHistoryRepository.save(
            AppearanceChargeHistoryEntity.removedFrom(
              appearanceCharge = appearanceCharge,
              removedBy = performedByUsername,
              removedPrison = null,
            ),
          )
          existingCharge.appearanceCharges.remove(appearanceCharge)
          chargeRecord.appearanceCharges.remove(appearanceCharge)
          appearance.appearanceCharges.remove(appearanceCharge)
          updatedCharge.appearanceCharges.remove(appearanceCharge)
          appearanceCharge.charge = null
          appearanceCharge.appearance = null
        }

      chargeRecord = chargeRepository.save(updatedCharge)
      val appearanceCharge = AppearanceChargeEntity(
        appearance,
        chargeRecord,
        performedByUsername,
        null,
      )
      appearance.appearanceCharges.add(appearanceCharge)
      chargeRecord.appearanceCharges.add(appearanceCharge)
      appearanceChargeHistoryRepository.save(AppearanceChargeHistoryEntity.from(appearanceCharge))
    } else {
      chargeModifyFunction(existingCharge)
    }
    chargeHistoryRepository.save(ChargeHistoryEntity.from(chargeRecord))
    return chargeRecord
  }
}
