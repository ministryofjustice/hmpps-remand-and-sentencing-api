package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.subjectaccessrequest.alldata

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.config.ConditionalOnSarEnabled
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ImmigrationDetentionRecordType
import java.time.LocalDate

@ConditionalOnSarEnabled
@Entity
@Immutable
@Table(name = "immigration_detention")
class ImmigrationDetentionSarEntity(

  @Id
  @Column
  var id: Int,
  var immigrationDetentionRecordType: ImmigrationDetentionRecordType,
  var prisonerId: String,
  var homeOfficeReferenceNumber: String?,
  var recordDate: LocalDate,
  var noLongerOfInterestReason: String?,
  var noLongerOfInterestComment: String?,
)
