package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document.mustache.helper

import com.github.jknack.handlebars.Helper
import com.github.jknack.handlebars.Options

class OptionalValueHelper : Helper<String> {

  override fun apply(value: String?, options: Options?): String = if (value.isNullOrEmpty()) "No Data Held" else value
}
