package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.DraftCreateCourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.DraftCreateCourtCase

class DraftDataCreator {
  companion object Factory {

    fun draftCreateCourtCase(prisonerId: String = "PRI123", draftAppearances: List<DraftCreateCourtAppearance> = listOf(draftCreateCourtAppearance())): DraftCreateCourtCase =
      DraftCreateCourtCase(prisonerId, draftAppearances)

    fun draftCreateCourtAppearance(sessionBlob: JsonNode = jacksonObjectMapper().createObjectNode().put("aFieldKey", "aFieldValue")): DraftCreateCourtAppearance =
      DraftCreateCourtAppearance(sessionBlob)
  }
}
