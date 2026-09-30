package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document

import java.time.LocalDate
import java.time.Period

class LodgeWarrants986(override val data: Data) : DocumentDetail<LodgeWarrants986.Data> {
  override val templateName: String = "lodge-warrants-986"

  data class Data(
    val name: String,
    val nomsNumber: String,
    val court: Court,
    val docGeneratedDate: LocalDate,
    val sentences: List<Sentence>,
    val telephoneNumber: String,
    val prisonName: String,
    val sentenceDate: LocalDate,
    val version: String,
  ) {
    fun totalFineAmount(): Double = this.sentences.sumOf { sentence -> sentence.fineAmount }
  }

  data class Sentence(val periodLength: Period, val offence: String, val fineAmount: Double)
  data class Court(val name: String, val premise: String, val street: String, val town: String, val county: String, val postalCode: String)
}
