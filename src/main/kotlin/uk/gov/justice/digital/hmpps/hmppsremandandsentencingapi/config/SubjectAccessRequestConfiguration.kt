package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.subjectaccessrequest.CourtCaseSarRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.subjectaccessrequest.ImmigrationDetentionSarRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.subjectaccessrequest.PrisonerDetailsService
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.subjectaccessrequest.UnsyncedPrisonerDetailsService

@Configuration
@ConditionalOnProperty(
  prefix = "hmpps.sar",
  name = ["enabled"],
  havingValue = "true",
  matchIfMissing = false,
)
class SubjectAccessRequestConfiguration {

  @Bean
  fun prisonerDetailsService(
    immigrationDetentionSarRepository: ImmigrationDetentionSarRepository,
    courtCaseSarRepository: CourtCaseSarRepository,
  ): PrisonerDetailsService {
    //TODO
    // if (allSarData) {
    // return AllPrisonerDetailsService(immigrationDetentionSarRepository)
    // }

    return UnsyncedPrisonerDetailsService(immigrationDetentionSarRepository, courtCaseSarRepository)
  }
}
