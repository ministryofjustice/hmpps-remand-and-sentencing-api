package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.subjectaccessrequest

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.ImmigrationDetention
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.Prisoner
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.SarContent
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.subjectaccessrequest.ImmigrationDetentionSarEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.subjectaccessrequest.CourtCaseSarRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.subjectaccessrequest.ImmigrationDetentionSarRepository
import java.time.LocalDate

class UnsyncedPrisonerDetailsService(
  private val immigrationDetentionUnsyncedSarRepository: ImmigrationDetentionSarRepository,
  private val courtCaseUnsyncedSarRepository: CourtCaseSarRepository,
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
    immigrationDetentionSarList: List<ImmigrationDetentionSarEntity>,
    from: LocalDate?,
    to: LocalDate?,
  ): MutableList<ImmigrationDetention> {
    val immigrationDetentions: MutableList<ImmigrationDetention> = ArrayList()

    for (ids in immigrationDetentionSarList.stream().filter { p: ImmigrationDetentionSarEntity -> filterByDate(from, to, p.recordDate) }.toList()) {
      immigrationDetentions.add(
        ImmigrationDetention(
          ids.homeOfficeReferenceNumber,
          ids.noLongerOfInterestReason,
          ids.noLongerOfInterestComment,
        ),
      )
    }
    return immigrationDetentions
  }
}
