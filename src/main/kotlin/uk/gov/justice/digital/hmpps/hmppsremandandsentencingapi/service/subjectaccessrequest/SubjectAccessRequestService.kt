package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.subjectaccessrequest

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import uk.gov.justice.hmpps.kotlin.sar.HmppsPrisonSubjectAccessRequestService
import uk.gov.justice.hmpps.kotlin.sar.HmppsSubjectAccessRequestContent
import java.time.LocalDate
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.Prisoner as Prisoner
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.alldata.Prisoner as AllDataPrisoner

@Service
@ConditionalOnProperty(
  prefix = "hmpps.sar",
  name = ["enabled"],
  havingValue = "true",
  matchIfMissing = false,
)
class SubjectAccessRequestService(
  @Value("\${hmpps.sar.mode.all-sar-data}") private val allSarData: Boolean,
  private val allDataPrisonerDetailsService: PrisonerDetailsService<AllDataPrisoner>,
  private val prisonerDetailsService: PrisonerDetailsService<Prisoner>,
) : HmppsPrisonSubjectAccessRequestService {

  override fun getPrisonContentFor(
    prn: String,
    fromDate: LocalDate?,
    toDate: LocalDate?,
  ): HmppsSubjectAccessRequestContent? {
    if (allSarData) {
      val allDataPrisonerDetails = allDataPrisonerDetailsService.getPrisonerDetails(prn, fromDate, toDate)
      return if (allDataPrisonerDetails != null) {
        HmppsSubjectAccessRequestContent(allDataPrisonerDetails, listOf())
      } else {
        null
      }
    }

    val prisonerDetails = prisonerDetailsService.getPrisonerDetails(prn, fromDate, toDate)
    return if (prisonerDetails != null) {
      HmppsSubjectAccessRequestContent(prisonerDetails, listOf())
    } else {
      null
    }
  }
}
