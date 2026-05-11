package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.subjectaccessrequest

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import org.hibernate.annotations.Immutable
import org.hibernate.annotations.Subselect
import org.hibernate.annotations.Synchronize
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.config.ConditionalOnSarEnabled
import java.time.LocalDate

@ConditionalOnSarEnabled
@Entity
@Immutable
@Subselect(
  """
  select id
  ,prisoner_id
  ,record_date
  ,home_office_reference_number 
  ,no_longer_of_interest_reason 
  ,no_longer_of_interest_comment 
  from immigration_detention""",
)
@Synchronize("immigration_detention")
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
