package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document

interface HtmlRenderer<T: DocumentDetail<*>> {
  fun render(documentDetail: T): String
}
