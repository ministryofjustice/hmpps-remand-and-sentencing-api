package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document.impl

import com.github.jknack.handlebars.Context
import com.github.jknack.handlebars.Handlebars
import org.jsoup.Jsoup
import org.jsoup.helper.W3CDom
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import org.w3c.dom.Document
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document.DocumentDetail
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document.HtmlRenderer
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Service
class MustacheHtmlRenderer<T : DocumentDetail<*>> : HtmlRenderer<T> {

  override fun render(documentDetail: T): Document {
    val handlebars = Handlebars()
    registerHelpers(handlebars)
    val context = Context
      .newBuilder(documentDetail.data)
      .build()
    val template = ClassPathResource("templates/" + documentDetail.templateName).file.readText()
    val compiledServiceTemplate = handlebars.compileInline(template)
    val html = compiledServiceTemplate.apply(context)
    val jsoupDocument = Jsoup.parse(html)
    val w3cDocument = W3CDom().fromJsoup(jsoupDocument)
    return w3cDocument
  }

  private fun registerHelpers(handlebars: Handlebars) {
    handlebars.registerHelper("formatDate") { value: Any?, _ ->
      when (value) {
        null -> null
        is LocalDate -> value.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        else -> value.toString()
      }
    }
    handlebars.registerHelper("formatCurrency") { value: Double?, _ ->
      value?.let { NumberFormat.getCurrencyInstance(Locale.UK).format(it) }
    }
  }
}
