package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document.impl

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document.DocumentDetail

class LodgeWarrants986(override val data: Data) : DocumentDetail<LodgeWarrants986.Data> {
  override val templateName: String = "lodge-warrants-986.mustache"

  data class Data(val name: String, val nomsNumber: String, val prisonNumber: String)
}
