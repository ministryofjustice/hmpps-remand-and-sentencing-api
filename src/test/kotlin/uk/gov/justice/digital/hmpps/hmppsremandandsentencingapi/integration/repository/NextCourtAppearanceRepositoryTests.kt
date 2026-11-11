package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.repository

import jakarta.persistence.EntityManager
import org.hibernate.Session
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase

class NextCourtAppearanceRepositoryTests : IntegrationTestBase() {

  @Autowired
  private lateinit var entityManager: EntityManager

  @Test
  fun `a single sql is executed when retrieving findFirstByPrisonerId`() {
    val (_, createdCourtCase) = createCourtCase()
    val session = entityManager.unwrap(Session::class.java)
    session.sessionFactory.statistics.isStatisticsEnabled = true
    val firstNextCourtAppearance = nextCourtAppearanceRepository.findFirstByPrisonerId(createdCourtCase.prisonerId)
    Assertions.assertNotNull(firstNextCourtAppearance)
    val statistics = session.sessionFactory.statistics
    Assertions.assertEquals(1, statistics.queryExecutionCount)
  }
}
