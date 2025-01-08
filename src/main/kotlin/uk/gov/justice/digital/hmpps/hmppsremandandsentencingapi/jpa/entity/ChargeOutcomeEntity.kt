package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import java.util.UUID

@Entity
@Table(name = "charge_outcome")
class ChargeOutcomeEntity(
  @Id
  @Column
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Int = 0,
  @Column
  val outcomeName: String,
  val outcomeUuid: UUID,
  val nomisCode: String,
  val outcomeType: String,
  val displayOrder: Int,
  val isSubList: Boolean,
  val dispositionCode: String,
  val chargeStatus: String,
) {
  fun getEntityStatus(): EntityStatus = if (chargeStatus == "ACTIVE") EntityStatus.ACTIVE else EntityStatus.INACTIVE
}
