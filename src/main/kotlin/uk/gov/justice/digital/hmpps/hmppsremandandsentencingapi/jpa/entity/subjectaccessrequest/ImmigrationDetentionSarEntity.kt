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
   ,immigration_detention_record_type  
  from immigration_detention
  where status_id != 'DELETED'
  """,
)
@Synchronize("immigration_detention")
class ImmigrationDetentionSarEntity(

  @Id
  @Column
  var id: Int,
  var prisonerId: String,
  var recordDate: LocalDate,
  var homeOfficeReferenceNumber: String?,
  var noLongerOfInterestReason: String?,
  var noLongerOfInterestComment: String?,
  var immigrationDetentionRecordType: String? = null,
)
