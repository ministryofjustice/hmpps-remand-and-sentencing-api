package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document

interface DocumentDetail<T> {
  fun getTemplateName(): String
  fun getData(): T
}