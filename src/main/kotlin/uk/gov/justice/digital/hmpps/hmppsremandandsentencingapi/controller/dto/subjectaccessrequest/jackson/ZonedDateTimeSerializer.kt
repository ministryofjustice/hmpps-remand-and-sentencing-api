package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.jackson

import tools.jackson.core.JsonGenerator
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.ValueSerializer
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class ZonedDateTimeSerializer : ValueSerializer<ZonedDateTime>() {

  override fun serialize(
    value: ZonedDateTime,
    gen: JsonGenerator,
    ctxt: SerializationContext,
  ) {
    gen.writeString(value.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:MM")))
  }
}
