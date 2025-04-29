package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.service

import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.SentenceTypeDetail
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.LegacySentenceTypeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceTypeClassification
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceTypePeriodDefinition
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.LegacySentenceTypeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacySentenceType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.model.LegacySentenceTypeGroupingSummary
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.model.RecallTypeIdentifier

@Service
class LegacySentenceTypesService(private val legacySentenceTypeRepository: LegacySentenceTypeRepository) {

  private val defaultSort: Sort = Sort.by(
    Sort.Order.desc("nomisActive"),
    Sort.Order.desc("nomisExpiryDate"),
    Sort.Order.asc("sentencingAct"),
  )

  fun getLegacySentencesByNomisSentenceTypeReference(nomisSentenceTypeReference: String): List<LegacySentenceType> = legacySentenceTypeRepository.findByNomisSentenceTypeReference(nomisSentenceTypeReference, defaultSort)
    .map(::toLegacySentenceType)

  fun getAllLegacySentences(): List<LegacySentenceType> = legacySentenceTypeRepository.findAll(defaultSort)
    .map(::toLegacySentenceType)

  fun getGroupedLegacySummaries(): List<LegacySentenceTypeGroupingSummary> = getAllLegacySentences()
    .groupBy { it.nomisSentenceTypeReference }
    .map { (key, group) -> summariseLegacySentenceGroup(key, group) }

  fun getLegacySentencesByNomisSentenceTypeReferenceAsSummary(nomisSentenceTypeReference: String): LegacySentenceTypeGroupingSummary = summariseLegacySentenceGroup(
    nomisSentenceTypeReference,
    getLegacySentencesByNomisSentenceTypeReference(nomisSentenceTypeReference),
  )

  private fun toLegacySentenceType(entity: LegacySentenceTypeEntity): LegacySentenceType = with(entity) {
    val classificationEnum = SentenceTypeClassification.from(classification)
    LegacySentenceType(
      id = id,
      classification = classificationEnum,
      classificationPeriodDefinition = SentenceTypePeriodDefinition.mapping[classificationEnum],
      nomisDescription = nomisDescription,
      nomisSentenceTypeReference = nomisSentenceTypeReference,
      nomisActive = nomisActive,
      nomisExpiryDate = nomisExpiryDate,
      eligibility = eligibility,
      recallType = RecallTypeIdentifier.from(recallTypeDescription).toDomain(),
      inputSentenceType = sentenceType?.let { SentenceTypeDetail.from(it) },
      nomisTermTypes = safeNomisTerms.associate { it.name to it.description },
      sentencingAct = sentencingAct,
    )
  }

  private fun summariseLegacySentenceGroup(
    nomisSentenceTypeReference: String,
    group: List<LegacySentenceType>,
  ): LegacySentenceTypeGroupingSummary {
    if (group.isEmpty()) {
      error("Group with reference $nomisSentenceTypeReference is empty")
    }

    val nomisDescription = group.first().nomisDescription
    val classification = group.first().classification
    val recallType = group.first().recallType

    if (group.any { it.classification != classification }) {
      error("Inconsistent classification for $nomisSentenceTypeReference: ${group.map { it.classification }}")
    }

    if (group.any { it.recallType != recallType }) {
      error("Inconsistent recallType for $nomisSentenceTypeReference: ${group.map { it.recallType }}")
    }

    val nomisActive = group.all { it.nomisActive }
    val nomisExpiryDate = group
      .mapNotNull { it.nomisExpiryDate }
      .minOrNull()

    return LegacySentenceTypeGroupingSummary(
      nomisSentenceTypeReference = nomisSentenceTypeReference,
      nomisDescription = nomisDescription,
      isIndeterminate = classification == SentenceTypeClassification.INDETERMINATE,
      recall = recallType,
      nomisActive = nomisActive,
      nomisExpiryDate = nomisExpiryDate,
    )
  }
}
