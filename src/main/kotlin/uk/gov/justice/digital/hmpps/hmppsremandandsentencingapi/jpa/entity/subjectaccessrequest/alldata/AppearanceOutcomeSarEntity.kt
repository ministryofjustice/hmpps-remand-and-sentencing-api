package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.subjectaccessrequest.alldata

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import org.hibernate.annotations.Subselect
import org.hibernate.annotations.Synchronize
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.config.ConditionalOnSarEnabled

@ConditionalOnSarEnabled
@Immutable
@Entity
@Subselect("select * from appearance_outcome")
@Synchronize("appearance_outcome")
@Table(name = "appearance_outcome")
class AppearanceOutcomeSarEntity(
  @Id
  @Column
  val id: Int,
  var outcomeName: String,
)
