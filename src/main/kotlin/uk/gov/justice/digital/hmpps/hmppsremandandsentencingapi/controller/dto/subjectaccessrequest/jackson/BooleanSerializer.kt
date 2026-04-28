package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.jackson

import tools.jackson.core.JsonGenerator
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.ValueSerializer

class BooleanSerializer : ValueSerializer<Boolean>() {

  override fun serialize(
    value: Boolean?,
    gen: JsonGenerator,
    ctxt: SerializationContext,
  ) {
    gen.writeString(if (value != null && value) "Yes" else "No")
  }
}
