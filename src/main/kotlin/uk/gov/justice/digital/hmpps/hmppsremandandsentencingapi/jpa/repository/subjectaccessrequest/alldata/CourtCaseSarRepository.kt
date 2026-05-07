package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.subjectaccessrequest.alldata

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.repository.CrudRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.config.ConditionalOnSarEnabled
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.subjectaccessrequest.alldata.CourtCaseSarEntity

@ConditionalOnSarEnabled
interface CourtCaseSarRepository : CrudRepository<CourtCaseSarEntity, Integer> {

  @EntityGraph(
    attributePaths = [
      "appearances",
      "appearances.appearanceCharges",
      "appearances.appearanceCharges.charge",
      "appearances.appearanceCharges.charge.chargeOutcome",
      "appearances.appearanceCharges.charge.sentences",
      "appearances.appearanceCharges.charge.sentences.sentenceType",
      "appearances.appearanceCharges.charge.sentences.periodLengths",
      "appearances.appearanceOutcome",
      "appearances.nextCourtAppearance",
      "latestCourtAppearance",
      "latestCourtAppearance.appearanceCharges",
      "latestCourtAppearance.appearanceCharges.charge",
      "latestCourtAppearance.appearanceCharges.charge.chargeOutcome",
      "latestCourtAppearance.appearanceCharges.charge.sentences",
      "latestCourtAppearance.appearanceCharges.charge.sentences.sentenceType",
      "latestCourtAppearance.appearanceCharges.charge.sentences.periodLengths",
      "latestCourtAppearance.appearanceOutcome",
      "latestCourtAppearance.nextCourtAppearance",
    ],
  )
  fun findByPrisonerId(prisonerId: String): List<CourtCaseSarEntity>

  fun existsByPrisonerId(prisonerId: String): Boolean
}
