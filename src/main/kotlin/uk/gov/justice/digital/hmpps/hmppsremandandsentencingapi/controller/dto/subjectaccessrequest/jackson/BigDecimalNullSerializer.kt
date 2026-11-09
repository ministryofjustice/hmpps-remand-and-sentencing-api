package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.jackson

import tools.jackson.core.JsonGenerator
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.ValueSerializer
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.jackson.JacksonConfig.NULL_DATA_DEFAULT
import java.math.BigDecimal

class BigDecimalNullSerializer : ValueSerializer<BigDecimal?>() {

  override fun serialize(
    value: BigDecimal?,
    gen: JsonGenerator,
    ctxt: SerializationContext,
  ) {
    gen.writeString(NULL_DATA_DEFAULT)
  }
}
