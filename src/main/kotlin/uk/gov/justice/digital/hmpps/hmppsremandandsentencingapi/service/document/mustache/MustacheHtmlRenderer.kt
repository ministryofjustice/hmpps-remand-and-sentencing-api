package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document.mustache

import com.github.jknack.handlebars.Context
import com.github.jknack.handlebars.Handlebars
import org.jsoup.Jsoup
import org.jsoup.helper.W3CDom
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import org.w3c.dom.Document
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document.DocumentDetail
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document.HtmlRenderer
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document.mustache.helper.FormatCurrencyHelper
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document.mustache.helper.FormatDateHelper
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document.mustache.helper.FormatPeriodHelper

@Service
class MustacheHtmlRenderer<T : DocumentDetail<*>> : HtmlRenderer<T> {

  override fun render(documentDetail: T): Document {
    val handlebars = Handlebars()
    registerHelpers(handlebars)
    val context = Context
      .newBuilder(documentDetail.data)
      .build()
    val template = ClassPathResource("templates/documents/${documentDetail.templateName}.mustache").file.readText()
    val compiledServiceTemplate = handlebars.compileInline(template)
    val html = compiledServiceTemplate.apply(context)
    val jsoupDocument = Jsoup.parse(html)
    val w3cDocument = W3CDom().fromJsoup(jsoupDocument)
    return w3cDocument
  }

  private fun registerHelpers(handlebars: Handlebars) {
    handlebars.registerHelper("formatDate", FormatDateHelper())
    handlebars.registerHelper("formatCurrency", FormatCurrencyHelper())
    handlebars.registerHelper("formatPeriod", FormatPeriodHelper())
  }
}
