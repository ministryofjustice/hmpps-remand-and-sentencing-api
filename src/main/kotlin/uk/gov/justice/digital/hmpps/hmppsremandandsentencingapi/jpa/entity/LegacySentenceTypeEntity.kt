package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.time.LocalDate

@Entity
@Table(name = "legacy_sentence_types")
data class LegacySentenceTypeEntity(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Int = 0,

  @Column(name = "classification", nullable = false)
  val classification: String,

  @Column(name = "sentencing_act", nullable = false)
  val sentencingAct: Int,

  @Column(name = "eligibility", columnDefinition = "jsonb", nullable = false)
  val eligibility: String,

  @Column(name = "recall_type")
  val recallType: String? = null,

  @OneToOne(optional = true, fetch = FetchType.LAZY)
  @JoinColumn(name = "sentence_type_uuid", referencedColumnName = "sentence_type_uuid")
  val sentenceType: SentenceTypeEntity? = null,

  @Column(name = "nomis_reference", nullable = false)
  val nomisSentenceTypeReference: String,

  @Column(name = "nomis_active", nullable = false)
  val nomisActive: Boolean,

  @Column(name = "nomis_expiry_date")
  val nomisExpiryDate: LocalDate? = null,

  @Column(name = "nomis_description", nullable = false)
  val nomisDescription: String,

  @Column(name = "nomis_terms", columnDefinition = "jsonb", nullable = true)
  val nomisTerms: String,

)
