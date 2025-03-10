package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import java.util.UUID

data class CreateCourtAppearanceResponse(val appearanceUuid: UUID) {
  companion object {
    fun from(appearanceUuid: UUID, createCourtAppearance: CreateCourtAppearance): CreateCourtAppearanceResponse = CreateCourtAppearanceResponse(appearanceUuid)
  }
}
