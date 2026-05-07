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
@Subselect("select * from charge_outcome")
@Synchronize("charge_outcome")
@Table(name = "charge_outcome")
class ChargeOutcomeSarEntity(
  @Id
  @Column
  var id: Int = 0,
  @Column
  var outcomeType: String,
  @Column
  var outcomeName: String,
)
