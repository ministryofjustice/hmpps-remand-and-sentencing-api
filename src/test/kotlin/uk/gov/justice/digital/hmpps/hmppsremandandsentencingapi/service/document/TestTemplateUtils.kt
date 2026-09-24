package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document

import org.w3c.dom.Document
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document.impl.MustacheHtmlRenderer

fun <T : DocumentDetail<*>> convertTemplateToHtml(documentDetail: T): Document {
  val mustache = MustacheHtmlRenderer<T>()
  val resp = mustache.render(documentDetail)
  return resp
}
