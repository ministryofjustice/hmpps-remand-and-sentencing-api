package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.service

import jakarta.persistence.EntityManager
import org.hibernate.Session
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.CacheManager
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.person.PersonNextCourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.EventSource
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.CourtAppearanceDomainEventService
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.NextCourtAppearanceService
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.Constants

class NextCourtAppearanceServiceTests : IntegrationTestBase() {

  @Autowired
  private lateinit var nextCourtAppearanceService: NextCourtAppearanceService

  @Autowired
  private lateinit var entityManager: EntityManager

  @Autowired
  private lateinit var cacheManager: CacheManager

  @Autowired
  private lateinit var courtAppearanceDomainEventService: CourtAppearanceDomainEventService

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

  @Test
  fun `inserts into the cache using prisoner ID as the key`() {
    val (_, createdCourtCase) = createCourtCase()
    Assertions.assertNull(cacheManager.getCache(Constants.NEXT_COURT_APPEARANCE_PERSON_CACHE_KEY)!!.get(createdCourtCase.prisonerId))
    val result = nextCourtAppearanceService.findFirstByPrisonerId(createdCourtCase.prisonerId)
    Assertions.assertEquals(result, cacheManager.getCache(Constants.NEXT_COURT_APPEARANCE_PERSON_CACHE_KEY)!!.get(createdCourtCase.prisonerId)!!.get())
  }

  @Test
  fun `evict cache when court appearance event emitted`() {
    val (_, createdCourtCase) = createCourtCase()
    // create
    checkEventEvictsCache(createdCourtCase.prisonerId) {
      courtAppearanceDomainEventService.create(createdCourtCase.prisonerId, "1", "1", EventSource.DPS, false, false)
    }
    checkEventEvictsCache(createdCourtCase.prisonerId) {
      courtAppearanceDomainEventService.update(createdCourtCase.prisonerId, "2", "2", EventSource.DPS, false, false)
    }
    checkEventEvictsCache(createdCourtCase.prisonerId) {
      courtAppearanceDomainEventService.delete(createdCourtCase.prisonerId, "1", "2", EventSource.DPS, false, false)
    }
    checkEventEvictsCache(createdCourtCase.prisonerId) {
      courtAppearanceDomainEventService.createBreach(
        createdCourtCase.prisonerId,
        "1",
        "1",
        setOf("1"),
        setOf("1"),
        listOf("1"),
        setOf("1"),
        EventSource.DPS,
      )
    }
  }

  @Test
  fun `can store nullable values in cache`() {
    val randomPrisonerId = "ARANDOMPRI1"
    nextCourtAppearanceService.findFirstByPrisonerId(randomPrisonerId)
    Assertions.assertNull(cacheManager.getCache(Constants.NEXT_COURT_APPEARANCE_PERSON_CACHE_KEY)!!.get(randomPrisonerId)!!.get())
  }

  private fun fillCache(prisonerId: String): PersonNextCourtAppearance {
    val result = nextCourtAppearanceService.findFirstByPrisonerId(prisonerId)!!
    Assertions.assertEquals(result, cacheManager.getCache(Constants.NEXT_COURT_APPEARANCE_PERSON_CACHE_KEY)!!.get(prisonerId)!!.get())
    return result
  }

  private fun checkEventEvictsCache(prisonerId: String, emitEvent: () -> Unit) {
    fillCache(prisonerId)
    emitEvent()
    Assertions.assertNull(cacheManager.getCache(Constants.NEXT_COURT_APPEARANCE_PERSON_CACHE_KEY)!!.get(prisonerId))
  }
}
