package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.subjectaccessrequest

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import org.hibernate.annotations.Immutable
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.annotations.Subselect
import org.hibernate.annotations.Synchronize
import org.hibernate.type.SqlTypes
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.config.ConditionalOnSarEnabled
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceEntityStatus
import java.time.LocalDate

@ConditionalOnSarEnabled
@Immutable
@Entity
@Subselect(
  """
  select id
  ,charge_outcome_id
  ,offence_code
  ,offence_start_date
  ,offence_end_date
  ,terror_related
  ,foreign_power_related
  ,domestic_violence_related
  ,legacy_data
  ,status_id
  from charge
  where status_id not in ('DELETED', 'DUPLICATE', 'FUTURE')""",
)
@Synchronize("charge")
class ChargeSarEntity(
  @Id
  var id: Int,
  @Suppress("JpaDataSourceORMInspection")
  @ManyToOne
  @JoinColumn(name = "charge_outcome_id")
  var chargeOutcome: ChargeOutcomeSarEntity?,
  @OneToMany(mappedBy = "charge")
  var sentences: MutableSet<SentenceSarEntity> = mutableSetOf(),
  var offenceCode: String,
  var offenceStartDate: LocalDate?,
  var offenceEndDate: LocalDate?,
  var terrorRelated: Boolean?,
  var foreignPowerRelated: Boolean?,
  var domesticViolenceRelated: Boolean?,
  @JdbcTypeCode(SqlTypes.JSON)
  var legacyData: ChargeLegacyDataSar? = null,
  var statusId: String,
) {
  fun getLiveSentence(): SentenceSarEntity? = sentences.firstOrNull { it.statusId != SentenceEntityStatus.DELETED.toString() }
}
