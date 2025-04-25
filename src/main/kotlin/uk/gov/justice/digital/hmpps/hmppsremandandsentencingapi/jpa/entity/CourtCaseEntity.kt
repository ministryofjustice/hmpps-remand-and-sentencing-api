package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.NamedAttributeNode
import jakarta.persistence.NamedEntityGraph
import jakarta.persistence.NamedSubgraph
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.annotations.BatchSize
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCourtCase
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
@NamedEntityGraph(
  name = "CourtCaseEntity.withAppearancesAndOutcomes",
  attributeNodes = [
    NamedAttributeNode("appearances", subgraph = "appearanceDetails"),
    NamedAttributeNode("latestCourtAppearance", subgraph = "appearanceDetails"),
  ],
  subgraphs = [
    NamedSubgraph(
      name = "appearanceDetails",
      attributeNodes = [
        NamedAttributeNode("appearanceOutcome"),
        NamedAttributeNode("periodLengths"),
        NamedAttributeNode("nextCourtAppearance", subgraph = "nextAppearanceDetails"),
        NamedAttributeNode("appearanceCharges", subgraph = "appearanceChargeChargeDetails"),
      ],
    ),
    NamedSubgraph(
      name = "nextAppearanceDetails",
      attributeNodes = [
        NamedAttributeNode("appearanceType"),
      ],
    ),
    NamedSubgraph(
      name = "appearanceChargeChargeDetails",
      attributeNodes = [
        NamedAttributeNode("charge", subgraph = "chargeDetails"),
      ],
    ),
    NamedSubgraph(
      name = "chargeDetails",
      attributeNodes = [
        NamedAttributeNode("chargeOutcome"),
        NamedAttributeNode("sentences", subgraph = "sentenceDetails"),
      ],
    ),
    NamedSubgraph(
      name = "sentenceDetails",
      attributeNodes = [
        NamedAttributeNode("sentenceType"),
        NamedAttributeNode("consecutiveTo"),
        NamedAttributeNode("periodLengths"),
      ],
    ),
  ],
)
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
  val createdBy: String,
  val createdPrison: String? = null,
  @Column
  @Enumerated(EnumType.ORDINAL)
  var statusId: EntityStatus,

  @JdbcTypeCode(SqlTypes.JSON)
  var legacyData: CourtCaseLegacyData? = null,

) {
  @OneToMany
  @JoinColumn(name = "court_case_id")
  @BatchSize(size = 50)
  var appearances: Set<CourtAppearanceEntity> = emptySet()

  @OneToMany(mappedBy = "courtCase")
  var draftAppearances: MutableList<DraftAppearanceEntity> = mutableListOf()

  @OneToOne
  @JoinColumn(name = "latest_court_appearance_id")
  var latestCourtAppearance: CourtAppearanceEntity? = null

  companion object {

    fun from(courtCase: CreateCourtCase, createdBy: String, caseUniqueIdentifier: String = UUID.randomUUID().toString()): CourtCaseEntity = CourtCaseEntity(prisonerId = courtCase.prisonerId, caseUniqueIdentifier = caseUniqueIdentifier, createdBy = createdBy, createdPrison = courtCase.prisonId, statusId = EntityStatus.ACTIVE, legacyData = courtCase.legacyData)

    fun from(courtCase: LegacyCreateCourtCase, createdByUsername: String): CourtCaseEntity = CourtCaseEntity(prisonerId = courtCase.prisonerId, caseUniqueIdentifier = UUID.randomUUID().toString(), createdBy = createdByUsername, statusId = if (courtCase.active) EntityStatus.ACTIVE else EntityStatus.INACTIVE)

    fun from(migrationCreateCourtCase: MigrationCreateCourtCase, createdByUsername: String, prisonerId: String): CourtCaseEntity = CourtCaseEntity(
      prisonerId = prisonerId,
      caseUniqueIdentifier = UUID.randomUUID().toString(),
      createdBy = createdByUsername,
      statusId = if (migrationCreateCourtCase.merged) {
        EntityStatus.MERGED
      } else if (migrationCreateCourtCase.active) {
        EntityStatus.ACTIVE
      } else {
        EntityStatus.INACTIVE
      },
      legacyData = migrationCreateCourtCase.courtCaseLegacyData,
    )

    fun from(draftCourtCase: DraftCreateCourtCase, createdByUsername: String): CourtCaseEntity = CourtCaseEntity(prisonerId = draftCourtCase.prisonerId, caseUniqueIdentifier = UUID.randomUUID().toString(), createdBy = createdByUsername, statusId = EntityStatus.DRAFT)
  }
}
