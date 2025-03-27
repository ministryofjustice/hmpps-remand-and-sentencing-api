package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.LegacySentenceType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.SentenceType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.LegacySentenceTypeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ReferenceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceTypeClassification
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceTypePeriodDefinition
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.LegacySentenceTypeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.SentenceTypeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.model.LegacySentenceTypeGroupingSummary
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.model.RecallType
import java.time.LocalDate
import java.util.UUID

@Service
class SentenceTypeService(
  private val sentenceTypeRepository: SentenceTypeRepository,
  private val legacySentenceTypeRepository: LegacySentenceTypeRepository
) {

  fun search(age: Int, convictionDate: LocalDate, statuses: List<ReferenceEntityStatus>, offenceDate: LocalDate): List<SentenceType> = sentenceTypeRepository.searchSentenceTypes(
    age,
    convictionDate,
    SentenceTypeClassification.LEGACY_RECALL,
    statuses,
    offenceDate,
  ).map { SentenceType.from(it) }

  fun findByUuid(sentenceTypeUuid: UUID): SentenceType? = sentenceTypeRepository.findBySentenceTypeUuid(sentenceTypeUuid)?.let { SentenceType.from(it) }

  fun findByUuids(uuids: List<UUID>): List<SentenceType> = sentenceTypeRepository.findBySentenceTypeUuidIn(uuids).map { SentenceType.from(it) }

  fun getLegacySentencesByNomisSentenceTypeReference(nomisSentenceTypeReference: String): List<LegacySentenceType> = legacySentenceTypeRepository.findByNomisSentenceTypeReference(nomisSentenceTypeReference)
    .map(::toLegacySentenceType)

  fun getAllLegacySentences(): List<LegacySentenceType> = legacySentenceTypeRepository.findAll()
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
      recallType = recallType?.let(RecallType::from),
      inputSentenceType = sentenceType?.let { SentenceType.from(it) },
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

    return LegacySentenceTypeGroupingSummary(
      nomisSentenceTypeReference = nomisSentenceTypeReference,
      nomisDescription = nomisDescription,
      isIndeterminate = classification == SentenceTypeClassification.INDETERMINATE,
      recall = recallType ?: RecallType.NONE,
    )
  }
}
