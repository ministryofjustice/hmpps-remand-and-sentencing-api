package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class RecallUtilTest {

  @Test
  fun `should not require UAL if there's no return to custody date`() {
    assertFalse(doesRecallRequireUAL(LocalDate.now(), null))
  }

  @Test
  fun `should not require UAL if the revocation and return to custody dates are the same`() {
    assertFalse(doesRecallRequireUAL(LocalDate.now(), LocalDate.now()))
  }

  @Test
  fun `should not require UAL if the return to custody date is one day after the revocation date`() {
    assertFalse(doesRecallRequireUAL(LocalDate.now(), LocalDate.now().plusDays(1)))
  }

  @Test
  fun `should require UAL if the return to custody date is more than one day after the revocation date`() {
    assertTrue(doesRecallRequireUAL(LocalDate.now(), LocalDate.now().plusDays(2)))
  }
}
