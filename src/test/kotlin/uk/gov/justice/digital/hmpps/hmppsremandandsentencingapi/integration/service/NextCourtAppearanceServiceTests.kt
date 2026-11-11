package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.service

import jakarta.persistence.EntityManager
import org.hibernate.Session
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.NextCourtAppearanceService

class NextCourtAppearanceServiceTests : IntegrationTestBase() {

  @Autowired
  private lateinit var nextCourtAppearanceService: NextCourtAppearanceService

  @Autowired
  private lateinit var entityManager: EntityManager

  @Test
  fun `a single sql is executed when retrieving findFirstByPrisonerId`() {
    val (_, createdCourtCase) = createCourtCase()
    val session = entityManager.unwrap(Session::class.java)
    session.sessionFactory.statistics.isStatisticsEnabled = true
    val firstNextCourtAppearance = nextCourtAppearanceService.findFirstByPrisonerId(createdCourtCase.prisonerId)
    Assertions.assertNotNull(firstNextCourtAppearance)
    val statistics = session.sessionFactory.statistics
    Assertions.assertEquals(1, statistics.prepareStatementCount)
  }
}
