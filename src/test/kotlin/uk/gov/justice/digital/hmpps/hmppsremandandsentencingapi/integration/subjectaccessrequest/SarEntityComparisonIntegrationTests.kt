package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.subjectaccessrequest

import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarIntegrationTestHelper
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarIntegrationTestHelperConfig
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarJpaEntitiesTest

/**
 * Compares the entire Entity Model to what's expected
 * in src/test/resources/sar/entity-schema-snapshot.json.
 *
 * This test will likely need to be updated regularly as the Entity Model changes,
 * but is purposefully designed to help identify whether this change should be included in the SAR model.
 */
@Import(SarIntegrationTestHelperConfig::class)
@TestPropertySource(
  properties = [
    "hmpps.sar.tests.expected-jpa-entity-schema.path=/sar/entity-schema-snapshot.json",
  ],
)
class SarEntityComparisonIntegrationTests :
  IntegrationTestBase(),
  SarJpaEntitiesTest {

  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  lateinit var sarIntegrationTestHelper: SarIntegrationTestHelper

  @Autowired
  lateinit var entityManager: EntityManager

  override fun getSarHelper(): SarIntegrationTestHelper = sarIntegrationTestHelper

  override fun getEntityManagerInstance(): EntityManager = entityManager
}
