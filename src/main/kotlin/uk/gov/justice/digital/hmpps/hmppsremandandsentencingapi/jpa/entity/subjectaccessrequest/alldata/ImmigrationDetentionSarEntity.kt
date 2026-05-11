package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.subjectaccessrequest.alldata

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
   ,immigration_detention_record_type
   ,prisoner_id
   ,home_office_reference_number
   ,record_date
   ,no_longer_of_interest_reason
   ,no_longer_of_interest_comment
  from immigration_detention""",
)
@Synchronize("immigration_detention")
class ImmigrationDetentionSarEntity(

  @Id
  @Column
  var id: Int,
  var immigrationDetentionRecordType: String?,
  var prisonerId: String,
  var homeOfficeReferenceNumber: String?,
  var recordDate: LocalDate,
  var noLongerOfInterestReason: String?,
  var noLongerOfInterestComment: String?,
)
