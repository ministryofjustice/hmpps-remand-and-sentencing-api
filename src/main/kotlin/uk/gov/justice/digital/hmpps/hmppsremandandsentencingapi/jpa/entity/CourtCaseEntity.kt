package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

@Entity
@Table(name = "court_case")
data class CourtCaseEntity(
  @Id
  @Column
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Int = 0,

  @Column
  val prisonerId: String,
  @Column
  val caseUniqueIdentifier: String,

  @Column
  val createdAt: ZonedDateTime = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS),
  @Column
  val createdByUsername: String,
  @Column
  @Enumerated(EnumType.ORDINAL)
  var statusId: EntityStatus,

) {
  @OneToMany
  @JoinColumn(name = "court_case_id")
  var appearances: List<CourtAppearanceEntity> = emptyList()

  @OneToOne
  @JoinColumn(name = "latest_court_appearance_id")
  var latestCourtAppearance: CourtAppearanceEntity? = null

  companion object {
    fun placeholderEntity(prisonerId: String, caseUniqueIdentifier: String = UUID.randomUUID().toString(), createdByUsername: String): CourtCaseEntity {
      return CourtCaseEntity(prisonerId = prisonerId, caseUniqueIdentifier = caseUniqueIdentifier, createdByUsername = createdByUsername, statusId = EntityStatus.ACTIVE)
    }
  }
}
