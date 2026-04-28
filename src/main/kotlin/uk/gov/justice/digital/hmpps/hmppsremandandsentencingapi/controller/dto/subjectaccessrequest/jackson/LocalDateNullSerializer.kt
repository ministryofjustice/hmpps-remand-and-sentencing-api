package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.jackson

import tools.jackson.core.JsonGenerator
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.ValueSerializer
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.jackson.JacksonConfig.NULL_DATA_DEFAULT
import java.time.LocalDate

class LocalDateNullSerializer : ValueSerializer<LocalDate?>() {

  override fun serialize(
    value: LocalDate?,
    gen: JsonGenerator,
    ctxt: SerializationContext,
  ) {
    gen.writeString(NULL_DATA_DEFAULT)
  }
}
