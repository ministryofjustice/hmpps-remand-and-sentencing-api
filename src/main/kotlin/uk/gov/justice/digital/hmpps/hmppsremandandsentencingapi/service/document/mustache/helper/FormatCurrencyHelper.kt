package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document.mustache.helper

import com.github.jknack.handlebars.Helper
import com.github.jknack.handlebars.Options
import java.text.NumberFormat
import java.util.Locale

class FormatCurrencyHelper : Helper<Double> {

  override fun apply(value: Double?, options: Options?): String? = value?.let { NumberFormat.getCurrencyInstance(Locale.UK).format(it) }
}
