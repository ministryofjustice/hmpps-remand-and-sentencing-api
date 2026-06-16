package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.DynamicUpdate
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.AggravatingFactorStatus

@DynamicUpdate
@Entity
@Table(name = "aggravating_factor")
class AggravatingFactorEntity(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Int = 0,

  @Column(nullable = false)
  val code: String,

  @Column(nullable = false)
  val title: String,

  @Column(nullable = true)
  val description: String? = null,

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  val status: AggravatingFactorStatus = AggravatingFactorStatus.ACTIVE,

  @Column(nullable = false)
  val displayOrder: Int,
)
