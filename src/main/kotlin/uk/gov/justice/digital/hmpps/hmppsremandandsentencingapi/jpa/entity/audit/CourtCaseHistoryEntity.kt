package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtCaseEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.ChargeLegacyData
import java.time.LocalDate
import java.time.ZonedDateTime

@Entity
@Table(name = "court_case_history")
class CourtCaseHistoryEntity(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Int = 0,
  val prisonerId: String,
  val caseUniqueIdentifier: String,
  val latestCourtAppearanceId: Int?,
  val createdAt: ZonedDateTime = ZonedDateTime.now(),
  val createdBy: String,
  val updatedAt: ZonedDateTime?,
  val updatedBy: String?,
  @Enumerated(EnumType.ORDINAL)
  var statusId: EntityStatus,
  @JdbcTypeCode(SqlTypes.JSON)
  var legacyData: ChargeLegacyData? = null,
  val createdPrison: String?,
  val mergedToCaseId: Int?,
  val mergedToDate: LocalDate? = null,
  @OneToOne
  @JoinColumn(name = "original_court_case_id")
  val originalCourtCase: CourtCaseEntity,
)