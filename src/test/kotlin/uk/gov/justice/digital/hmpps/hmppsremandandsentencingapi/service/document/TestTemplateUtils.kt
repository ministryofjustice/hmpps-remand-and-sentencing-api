package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document

import org.springframework.core.io.ClassPathResource

fun <D, T : DocumentDetail<D>> convertTemplateToHtml(template: String, data: D): String {
  val mustache = MustacheHtmlRenderer<D, T>()
  val template = ClassPathResource("templates/$template").file.readText()
  val resp = mustache.render(template, data)
  return resp
}
