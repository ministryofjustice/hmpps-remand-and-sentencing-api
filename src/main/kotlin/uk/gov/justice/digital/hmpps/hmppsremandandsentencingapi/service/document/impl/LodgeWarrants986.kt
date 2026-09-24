package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document.impl

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document.DocumentDetail

class LodgeWarrants986(override val data: Data) : DocumentDetail<LodgeWarrants986.Data> {
  override val templateName: String = "lodge-warrants-986.mustache"

  data class Data(
    val name: String,
    val nomsNumber: String,
    val prisonNumber: String,
    val courtName: String,
    val date: String,
    val sentences: List<Sentence>,
    val telephoneNumber: String,
    val prisonName: String,
  )

  data class Sentence(val periodLength: String, val offence: String)
}
