package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.subjectaccessrequest

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import org.hibernate.annotations.Subselect
import org.hibernate.annotations.Synchronize
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.config.ConditionalOnSarEnabled
import java.time.LocalDate

@ConditionalOnSarEnabled
@Entity
@Immutable
@Subselect("select * from immigration_detention")
@Synchronize("immigration_detention")
@Table(name = "immigration_detention")
class ImmigrationDetentionUnsyncedSarEntity(

  @Id
  @Column
  var id: Int = 0,
  var prisonerId: String,
  var recordDate: LocalDate,
  var homeOfficeReferenceNumber: String?,
  var noLongerOfInterestReason: String?,
  var noLongerOfInterestComment: String?,
)
