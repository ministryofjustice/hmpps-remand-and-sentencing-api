package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.subjectaccessrequest

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.ImmigrationDetention
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.Prisoner
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.SarContent
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.subjectaccessrequest.ImmigrationDetentionSarEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.subjectaccessrequest.CourtCaseSarRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.subjectaccessrequest.ImmigrationDetentionSarRepository
import java.time.LocalDate

class UnsyncedPrisonerDetailsService(
  private val immigrationDetentionSarRepository: ImmigrationDetentionSarRepository,
  private val courtCaseSarRepository: CourtCaseSarRepository,
) : PrisonerDetailsService {
  override fun getPrisonerDetails(
    prisonerNumber: String,
    from: LocalDate?,
    to: LocalDate?,
  ): SarContent? = courtCaseSarRepository.existsByPrisonerId(prisonerNumber).takeIf { it }?.let {
    val immigrationDetentionsEntities = immigrationDetentionSarRepository.findImmigrationDetentionSarEntitiesByPrisonerId(prisonerNumber)
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

//  private fun filterByDate(from: LocalDate?, to: LocalDate?, toCompare: LocalDate?): Boolean = (from == null && to == null) ||
//    toCompare != null &&
//    (
//      (
//        (to != null && from != null &&
//          isEqualOrAfter(from = from, toCompare = toCompare) &&
//          isEqualOrBefore(to = to, toCompare = toCompare,)
//        ) ||
//        (to == null && from != null && isEqualOrAfter(from, toCompare)) ||
//        (to != null && from == null && isEqualOrBefore(to, toCompare))
//        )
//      )
//
//  private fun isEqualOrAfter(from: LocalDate, toCompare: LocalDate): Boolean =
//    toCompare.isEqual(from) || toCompare.isAfter(from)
//
//  private fun isEqualOrBefore(to: LocalDate, toCompare: LocalDate): Boolean =
//    toCompare.isEqual(to) || toCompare.isBefore(to)

  private fun filterByDate(from: LocalDate?, to: LocalDate?, toCompare: LocalDate?): Boolean {
    if (from == null && to == null) return true
    if (toCompare == null) return false
    return (from == null || toCompare >= from) && (to == null || toCompare <= to)
  }
}
