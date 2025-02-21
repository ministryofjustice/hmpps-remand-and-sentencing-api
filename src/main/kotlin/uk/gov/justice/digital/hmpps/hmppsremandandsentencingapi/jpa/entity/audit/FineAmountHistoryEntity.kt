package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import java.math.BigDecimal
import java.time.ZonedDateTime

@Entity
@Table(name = "fine_amount_history")
class FineAmountHistoryEntity(
  @Id
  @Column
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Int = 0,
  val fineAmount: BigDecimal,
  val sentenceId: Int,
  @Enumerated(EnumType.ORDINAL)
  val statusId: EntityStatus,
  val createdAt: ZonedDateTime,
  val createdBy: String,
  val createdPrison: String?,
  val updatedAt: ZonedDateTime?,
  val updatedBy: String?,
  val updatedPrison: String?,
)
