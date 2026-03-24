package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ReferenceEntityStatus
import java.util.*

@Entity
@Table(name = "court_appearance_subtype")
class CourtAppearanceSubtypeEntity(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Int = 0,
  val appearanceSubtypeUuid: UUID,
  val description: String,
  val displayOrder: Int,
  @Enumerated(EnumType.STRING)
  val status: ReferenceEntityStatus,
  val nomisCode: String,
  @OneToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "appearance_type_id")
  val appearanceType: AppearanceTypeEntity,
)
