package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document

import org.w3c.dom.Document

interface HtmlRenderer {
  fun <T : DocumentDetail<*>> render(documentDetail: T): Document
}
