package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.custom

import jakarta.persistence.EntityManager
import org.springframework.stereotype.Repository

@Repository
class CustomPrisonerDataRepositoryImpl(
  private val entityManager: EntityManager,
) : CustomPrisonerDataRepository {

  // This uses Native sql to delete all data for a prisoner - Only to be used for Migration
  override fun deletePrisonerData(prisonerId: String) {
    val sql = this::class.java.getResource("/sql/delete-prisoner.sql")?.readText()
      ?: throw IllegalStateException("Delete prisoner SQL file not found")
    entityManager.createNativeQuery(sql)
      .setParameter("prisonerId", prisonerId)
      .executeUpdate()
  }
}
