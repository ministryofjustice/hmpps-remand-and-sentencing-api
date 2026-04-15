package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Formula
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.appearancetype.AppearanceTypeCodes
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ReferenceEntityStatus
import java.util.UUID

@Entity
@Table(name = "appearance_type")
class AppearanceTypeEntity(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Int = 0,
  val appearanceTypeUuid: UUID,
  val description: String,
  val displayOrder: Int,
  @Enumerated(EnumType.STRING)
  val status: ReferenceEntityStatus,
  @JdbcTypeCode(SqlTypes.JSON)
  val nomisToDpsMappingCodes: AppearanceTypeCodes,
  val dpsToNomisMappingCode: String,
  @Formula("(select count(*) from court_appearance_subtype cas where cas.appearance_type_id= id)")
  val totalSubtypes: Int = -1,
)
