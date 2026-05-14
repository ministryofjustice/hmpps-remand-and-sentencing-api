package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.subjectaccessrequest

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.config.ConditionalOnSarEnabled
import uk.gov.justice.hmpps.kotlin.sar.HmppsPrisonSubjectAccessRequestService
import uk.gov.justice.hmpps.kotlin.sar.HmppsSubjectAccessRequestContent
import java.time.LocalDate

@Service
@ConditionalOnSarEnabled
class SubjectAccessRequestService(
  private val dataPrisonerDetailsService: PrisonerDetailsService,
) : HmppsPrisonSubjectAccessRequestService {

  override fun getPrisonContentFor(
    prn: String,
    fromDate: LocalDate?,
    toDate: LocalDate?,
  ): HmppsSubjectAccessRequestContent? = dataPrisonerDetailsService.getPrisonerDetails(prn, fromDate, toDate)?.let {
    log.debug("SAR Requested for {}", it.prisonerNumber)
    HmppsSubjectAccessRequestContent(it, listOf())
  }

  private companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
