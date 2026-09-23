package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document

interface HtmlRenderer<T> {
  fun render(templateName: String, data: T): String
}