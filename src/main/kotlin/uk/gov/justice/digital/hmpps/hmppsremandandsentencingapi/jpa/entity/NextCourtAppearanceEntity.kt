package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateNextCourtAppearance
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
  @Column
  val appearanceType: String,
) {
  fun isSame(other: NextCourtAppearanceEntity?): Boolean {
    return other != null && appearanceDate.isEqual(other.appearanceDate) &&
      courtCode == other.courtCode &&
      appearanceType == other.appearanceType
  }

  companion object {
    fun from(nextCourtAppearance: CreateNextCourtAppearance): NextCourtAppearanceEntity {
      return NextCourtAppearanceEntity(
        appearanceDate = nextCourtAppearance.appearanceDate,
        appearanceTime = nextCourtAppearance.appearanceTime,
        courtCode = nextCourtAppearance.courtCode,
        appearanceType = nextCourtAppearance.appearanceType,
      )
    }
  }
}
