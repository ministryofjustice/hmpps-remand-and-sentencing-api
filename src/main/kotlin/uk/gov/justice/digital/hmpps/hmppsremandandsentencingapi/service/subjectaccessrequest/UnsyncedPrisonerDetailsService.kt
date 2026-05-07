package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.subjectaccessrequest

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.ImmigrationDetention
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.Prisoner
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.SarContent
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.subjectaccessrequest.ImmigrationDetentionUnsyncedSarEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.subjectaccessrequest.CourtCaseUnsyncedSarRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.subjectaccessrequest.ImmigrationDetentionUnsyncedSarRepository
import java.time.LocalDate

class UnsyncedPrisonerDetailsService(
  private val immigrationDetentionUnsyncedSarRepository: ImmigrationDetentionUnsyncedSarRepository,
  private val courtCaseUnsyncedSarRepository: CourtCaseUnsyncedSarRepository,
) : PrisonerDetailsService {
  override fun getPrisonerDetails(
    prisonerNumber: String,
    from: LocalDate?,
    to: LocalDate?,
  ): SarContent? = courtCaseUnsyncedSarRepository.existsByPrisonerId(prisonerNumber).takeIf { it }?.let {
    val immigrationDetentionsEntities = immigrationDetentionUnsyncedSarRepository.findByPrisonerId(prisonerNumber)
    val immigrationDetentions = mapImmigrationDetentionsNotInNomis(immigrationDetentionsEntities, from, to)
    Prisoner(prisonerNumber, immigrationDetentions)
  }

  private fun mapImmigrationDetentionsNotInNomis(
    immigrationDetentionSarList: List<ImmigrationDetentionUnsyncedSarEntity>,
    from: LocalDate?,
    to: LocalDate?,
  ): MutableList<ImmigrationDetention> {
    val immigrationDetentions: MutableList<ImmigrationDetention> = ArrayList()

    for (ids in immigrationDetentionSarList.stream().filter { p: ImmigrationDetentionUnsyncedSarEntity -> filterByDate(from, to, p.recordDate) }.toList()) {
      // for (ids in immigrationDetentionSarList.stream().toList()) {
      immigrationDetentions.add(
        ImmigrationDetention(
          ids.homeOfficeReferenceNumber,
          ids.noLongerOfInterestReason.toString(),
          ids.noLongerOfInterestComment,
        ),
      )
    }
    return immigrationDetentions
  }

  private fun filterByDate(from: LocalDate?, to: LocalDate?, toCompare: LocalDate?): Boolean {
    if (from == null && to == null) return true
    if (toCompare == null) return false
    return (from == null || toCompare >= from) && (to == null || toCompare <= to)
  }
}
