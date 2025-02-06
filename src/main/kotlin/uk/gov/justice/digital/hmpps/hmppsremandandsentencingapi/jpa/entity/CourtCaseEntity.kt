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
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.DraftCreateCourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.CourtCaseLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCreateCourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateCourtCase
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

@Entity
@Table(name = "court_case")
class CourtCaseEntity(
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

  @JdbcTypeCode(SqlTypes.JSON)
  var legacyData: CourtCaseLegacyData? = null,

) {
  @OneToMany
  @JoinColumn(name = "court_case_id")
  var appearances: List<CourtAppearanceEntity> = emptyList()

  @OneToMany(mappedBy = "courtCase")
  var draftAppearances: MutableList<DraftAppearanceEntity> = mutableListOf()

  @OneToOne
  @JoinColumn(name = "latest_court_appearance_id")
  var latestCourtAppearance: CourtAppearanceEntity? = null

  companion object {
    fun placeholderEntity(prisonerId: String, caseUniqueIdentifier: String = UUID.randomUUID().toString(), createdByUsername: String, legacyData: CourtCaseLegacyData?): CourtCaseEntity = CourtCaseEntity(prisonerId = prisonerId, caseUniqueIdentifier = caseUniqueIdentifier, createdByUsername = createdByUsername, statusId = EntityStatus.ACTIVE, legacyData = legacyData)

    fun from(courtCase: LegacyCreateCourtCase, createdByUsername: String): CourtCaseEntity = CourtCaseEntity(prisonerId = courtCase.prisonerId, caseUniqueIdentifier = UUID.randomUUID().toString(), createdByUsername = createdByUsername, statusId = if (courtCase.active) EntityStatus.ACTIVE else EntityStatus.INACTIVE)

    fun from(migrationCreateCourtCase: MigrationCreateCourtCase, createdByUsername: String): CourtCaseEntity = CourtCaseEntity(prisonerId = migrationCreateCourtCase.prisonerId, caseUniqueIdentifier = UUID.randomUUID().toString(), createdByUsername = createdByUsername, statusId = if (migrationCreateCourtCase.active) EntityStatus.ACTIVE else EntityStatus.INACTIVE, legacyData = migrationCreateCourtCase.courtCaseLegacyData)

    fun from(draftCourtCase: DraftCreateCourtCase, createdByUsername: String): CourtCaseEntity = CourtCaseEntity(prisonerId = draftCourtCase.prisonerId, caseUniqueIdentifier = UUID.randomUUID().toString(), createdByUsername = createdByUsername, statusId = EntityStatus.DRAFT)
  }
}
