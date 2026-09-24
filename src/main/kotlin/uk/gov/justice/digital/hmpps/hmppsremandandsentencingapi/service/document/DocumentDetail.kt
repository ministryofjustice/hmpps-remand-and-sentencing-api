package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document

interface DocumentDetail<T> {
  val templateName: String
  val data: T
}
