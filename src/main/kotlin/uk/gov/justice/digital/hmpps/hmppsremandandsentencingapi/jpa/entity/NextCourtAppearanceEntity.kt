package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateNextCourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCreateCourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateCourtAppearance
import java.time.LocalDate
import java.time.LocalTime

@Entity
@Table(name = "next_court_appearance")
class NextCourtAppearanceEntity(
  @Id
  @Column
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Int = 0,
  @Column
  val appearanceDate: LocalDate,
  @Column
  val appearanceTime: LocalTime?,
  @Column
  val courtCode: String,
  @OneToOne
  @JoinColumn(name = "appearance_type_id")
  val appearanceType: AppearanceTypeEntity,
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "future_skeleton_appearance_id")
  val futureSkeletonAppearance: CourtAppearanceEntity,
) {
  fun isSame(other: NextCourtAppearanceEntity?): Boolean = other != null &&
    appearanceDate.isEqual(other.appearanceDate) &&
    ((appearanceTime == null && other.appearanceTime == null) || appearanceTime == other.appearanceTime) &&
    courtCode == other.courtCode &&
    appearanceType == other.appearanceType

  fun copyFrom(nomisAppearance: LegacyCreateCourtAppearance, futureAppearance: CourtAppearanceEntity, appearanceTypeEntity: AppearanceTypeEntity): NextCourtAppearanceEntity = NextCourtAppearanceEntity(
    id,
    nomisAppearance.appearanceDate,
    nomisAppearance.legacyData.appearanceTime,
    nomisAppearance.courtCode,
    appearanceTypeEntity,
    futureAppearance,
  )

  companion object {
    fun from(nextCourtAppearance: CreateNextCourtAppearance, futureSkeletonAppearance: CourtAppearanceEntity, appearanceTypeEntity: AppearanceTypeEntity): NextCourtAppearanceEntity = NextCourtAppearanceEntity(
      appearanceDate = nextCourtAppearance.appearanceDate,
      appearanceTime = nextCourtAppearance.appearanceTime,
      courtCode = nextCourtAppearance.courtCode,
      appearanceType = appearanceTypeEntity,
      futureSkeletonAppearance = futureSkeletonAppearance,
    )

    fun from(nomisAppearance: MigrationCreateCourtAppearance, nomisFutureAppearance: MigrationCreateCourtAppearance, futureAppearance: CourtAppearanceEntity, appearanceTypeEntity: AppearanceTypeEntity): NextCourtAppearanceEntity = NextCourtAppearanceEntity(
      appearanceDate = nomisAppearance.legacyData.nextEventDateTime!!.toLocalDate(),
      appearanceTime = nomisAppearance.legacyData.nextEventDateTime.toLocalTime(),
      courtCode = nomisFutureAppearance.courtCode,
      appearanceType = appearanceTypeEntity,
      futureSkeletonAppearance = futureAppearance,
    )

    fun from(nomisAppearance: LegacyCreateCourtAppearance, futureAppearance: CourtAppearanceEntity, appearanceTypeEntity: AppearanceTypeEntity): NextCourtAppearanceEntity = NextCourtAppearanceEntity(
      appearanceDate = nomisAppearance.appearanceDate,
      appearanceTime = nomisAppearance.legacyData.appearanceTime,
      courtCode = nomisAppearance.courtCode,
      appearanceType = appearanceTypeEntity,
      futureSkeletonAppearance = futureAppearance,
    )

    fun from(futureAppearance: CourtAppearanceEntity, appearanceTypeEntity: AppearanceTypeEntity): NextCourtAppearanceEntity = NextCourtAppearanceEntity(
      appearanceDate = futureAppearance.appearanceDate,
      appearanceTime = null,
      courtCode = futureAppearance.courtCode,
      appearanceType = appearanceTypeEntity,
      futureSkeletonAppearance = futureAppearance,
    )
  }
}
