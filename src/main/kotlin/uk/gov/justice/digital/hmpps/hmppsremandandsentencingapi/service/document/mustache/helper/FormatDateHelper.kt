package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document.mustache.helper

import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class FormatDateHelper : Helper<LocalDate> {

  override fun apply(value: LocalDate?, options: Options?): String? {
    return value?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
  }

}