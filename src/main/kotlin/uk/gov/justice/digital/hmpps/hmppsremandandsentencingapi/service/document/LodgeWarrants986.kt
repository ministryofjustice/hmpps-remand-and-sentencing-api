package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document

class LodgeWarrants986(val courtAppearanceData: LodgeWarrants986Data) : DocumentDetail<LodgeWarrants986Data> {
  override fun getTemplateName(): String = "lodge-warrants-986.mustache"

  override fun getData(): LodgeWarrants986Data {
    return this.courtAppearanceData
  }
}

data class LodgeWarrants986Data(val name: String, val nomsNumber: String, val prisonNumber: String)