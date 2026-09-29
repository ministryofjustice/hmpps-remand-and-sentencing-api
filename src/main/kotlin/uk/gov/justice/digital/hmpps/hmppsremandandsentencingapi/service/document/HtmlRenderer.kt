package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document

import org.w3c.dom.Document

interface HtmlRenderer<T : DocumentDetail<*>> {
  fun render(documentDetail: T): Document
}
