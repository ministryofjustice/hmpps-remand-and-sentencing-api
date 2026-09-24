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

@Service
class MustacheHtmlRenderer<T : DocumentDetail<*>> : HtmlRenderer<T> {

  override fun render(documentDetail: T): Document {
    val handlebars = Handlebars()
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
}
