package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.schemaspy

import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase

class InitialiseDatabase : IntegrationTestBase() {

  @Test
  fun `initialises database`() {
    log.debug("Database has been initialised by IntegrationTestBase")
  }

  private companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
