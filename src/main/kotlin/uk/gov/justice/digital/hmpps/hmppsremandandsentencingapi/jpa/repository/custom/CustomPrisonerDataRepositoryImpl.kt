package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.custom

import jakarta.persistence.EntityManager
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CustomPrisonerDataRepository

@Repository
class CustomPrisonerDataRepositoryImpl(
  private val entityManager: EntityManager,
) : CustomPrisonerDataRepository {

  override fun deletePrisonerData(prisonerId: String) {
    val sql = this::class.java.getResource("/sql/delete-prisoner.sql")!!.readText()
    entityManager.createNativeQuery(sql)
      .setParameter("prisonerId", prisonerId)
      .executeUpdate()
  }
}
