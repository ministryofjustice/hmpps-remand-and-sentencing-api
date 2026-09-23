package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document

import com.github.jknack.handlebars.Context
import com.github.jknack.handlebars.Handlebars
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Entities
import org.springframework.stereotype.Service

@Service
class MustacheHtmlRenderer<T> : HtmlRenderer<T> {

  override fun render(templateName: String, data: T): String {
    val handlebars = Handlebars()
    val context = Context
      .newBuilder(data)
      .build()
    val compiledServiceTemplate = handlebars.compileInline(templateName)
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