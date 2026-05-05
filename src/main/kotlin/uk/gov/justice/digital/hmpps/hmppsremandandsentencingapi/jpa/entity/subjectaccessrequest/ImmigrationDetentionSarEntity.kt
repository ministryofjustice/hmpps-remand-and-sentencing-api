package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.subjectaccessrequest

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ImmigrationDetentionNoLongerOfInterestType
import java.time.LocalDate

@Entity
@Immutable
@Table(name = "immigration_detention")
class ImmigrationDetentionSarEntity(

  @Id
  @Column
  var id: Int = 0,
  var prisonerId: String,
  var recordDate: LocalDate,
  var homeOfficeReferenceNumber: String?,
  @Enumerated(EnumType.STRING)
  var noLongerOfInterestReason: ImmigrationDetentionNoLongerOfInterestType?,
  var noLongerOfInterestComment: String?,
)
