package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document

interface HtmlRenderer<T, U : DocumentDetail<T>> {
  fun render(templateName: String, data: T): String
}
