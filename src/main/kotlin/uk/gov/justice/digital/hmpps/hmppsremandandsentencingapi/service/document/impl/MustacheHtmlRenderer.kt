package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document.impl

import com.github.jknack.handlebars.Context
import com.github.jknack.handlebars.Handlebars
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Entities
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document.DocumentDetail
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document.HtmlRenderer

@Service
class MustacheHtmlRenderer<T : DocumentDetail<*>> : HtmlRenderer<T> {

  override fun render(documentDetail: T): String {
    val handlebars = Handlebars()
    val context = Context
      .newBuilder(documentDetail.data)
      .build()
    val template = ClassPathResource("templates/" + documentDetail.templateName).file.readText()
    val compiledServiceTemplate = handlebars.compileInline(template)
    val renderedServiceReport = compiledServiceTemplate.apply(context)

    return convertToXhtml(renderedServiceReport)
  }

  private fun convertToXhtml(html: String): String {
    val serviceFragment = Jsoup.parseBodyFragment(html)

    serviceFragment.outputSettings()
      .syntax(Document.OutputSettings.Syntax.xml)
      .escapeMode(Entities.EscapeMode.xhtml)
      .charset(Charsets.UTF_8)
      .prettyPrint(true)

    return serviceFragment.toString()
  }
}
