package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document.impl.MustacheHtmlRenderer

fun <T : DocumentDetail<*>> convertTemplateToHtml(documentDetail: T): String {
  val mustache = MustacheHtmlRenderer<T>()
  val resp = mustache.render(documentDetail)
  return resp
}
