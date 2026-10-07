package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document.dto.PeriodLength
import java.time.LocalDate

class F986(override val data: Data) : DocumentDetail<F986.Data> {
  override val templateName: String = "f986"

  data class Data(
    val name: String,
    val nomsNumber: String,
    val court: Court,
    val docGeneratedDate: LocalDate,
    val sentences: List<Sentence>,
    val telephoneNumber: String,
    val prisonName: String?,
    val sentenceDate: LocalDate,
    val version: String,
  ) {
    fun totalFineAmount(): Double = this.sentences.sumOf { sentence -> sentence.fineAmount }
  }

  data class Sentence(val termLengths: List<PeriodLength>, val offence: String, val fineAmount: Double)
  data class Court(val name: String?, val premise: String, val street: String, val town: String, val county: String, val postalCode: String)
}
