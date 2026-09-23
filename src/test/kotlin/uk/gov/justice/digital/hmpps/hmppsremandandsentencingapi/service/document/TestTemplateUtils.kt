package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document

import org.springframework.core.io.ClassPathResource

fun <T> convertTemplateToHtml(template: String, data: T): String {
  val mustache = MustacheHtmlRenderer<T>()
  val template = ClassPathResource("templates/$template").file.readText()
  val resp = mustache.render(template, data)
  return resp
}