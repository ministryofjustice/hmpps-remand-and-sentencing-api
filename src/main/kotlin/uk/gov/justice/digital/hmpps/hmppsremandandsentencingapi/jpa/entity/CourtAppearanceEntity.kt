package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

@Entity
@Table(name = "court_appearance")
data class CourtAppearanceEntity(
  @Id
  @Column
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Int? = null,
  @Column
  val appearanceUuid: UUID,

  @ManyToOne
  @JoinColumn(name = "appearance_outcome_id")
  val appearanceOutcome: AppearanceOutcomeEntity,

  @ManyToOne
  @JoinColumn(name = "court_case_id")
  val courtCase: CourtCaseEntity,

  @Column
  val courtCode: String,
  @Column
  val courtCaseReference: String,
  @Column
  val appearanceDate: LocalDate,
  @Column
  @Enumerated(EnumType.ORDINAL)
  var statusId: EntityStatus,
  @OneToOne
  @JoinColumn(name = "previous_appearance_id")
  var previousAppearance: CourtAppearanceEntity?,

  @Column
  val warrantId: String,
  @Column
  val createdAt: ZonedDateTime = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS),
  @Column
  val createdByUsername: String,
  @Column
  val createdPrison: String,
  @OneToMany(mappedBy = "courtAppearance")
  val charges: Set<AppearanceChargeEntity>,
)
