package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.Prisoner
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.alldata.AllDataPrisoner
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.subjectaccessrequest.PrisonerDetailsService
import java.time.LocalDate

@Configuration
@ConditionalOnProperty(
  prefix = "hmpps.sar",
  name = ["enabled"],
  havingValue = "true",
  matchIfMissing = false,
)
class SubjectAccessRequestConfiguration {

  @Bean
  fun allDataPrisonerDetailsService(): PrisonerDetailsService<AllDataPrisoner> = object : PrisonerDetailsService<AllDataPrisoner> {
    override fun getPrisonerDetails(prisonerNumber: String, from: LocalDate?, to: LocalDate?): AllDataPrisoner? {
      TODO("Not yet implemented")
    }
  }

  @Bean
  fun prisonerDetailsService(): PrisonerDetailsService<Prisoner> = object : PrisonerDetailsService<Prisoner> {
    override fun getPrisonerDetails(prisonerNumber: String, from: LocalDate?, to: LocalDate?): Prisoner? {
      TODO("Not yet implemented")
    }
  }
}
