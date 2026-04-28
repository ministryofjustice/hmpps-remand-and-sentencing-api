package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.jackson

import tools.jackson.core.JsonGenerator
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.ValueSerializer
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class LocalDateSerializer : ValueSerializer<LocalDate>() {

  override fun serialize(
    value: LocalDate,
    gen: JsonGenerator,
    ctxt: SerializationContext,
  ) {
    gen.writeString(value.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
  }
}
