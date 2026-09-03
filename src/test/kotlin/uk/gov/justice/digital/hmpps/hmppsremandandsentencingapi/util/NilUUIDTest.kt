package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.Constants.Companion.nilUUID

class NilUUIDTest {

  @Test
  fun `Nil UUID generates a valid nil UUID`() {
    assertThat(nilUUID.toString()).isEqualTo("00000000-0000-0000-0000-00000000000")
  }
}
