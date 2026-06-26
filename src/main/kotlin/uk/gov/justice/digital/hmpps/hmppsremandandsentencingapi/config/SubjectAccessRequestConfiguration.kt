package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.subjectaccessrequest.CourtCaseSarRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.subjectaccessrequest.ImmigrationDetentionSarRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.subjectaccessrequest.RecallSarRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.CourtRegisterService
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.PersonService
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.subjectaccessrequest.PrisonerDetailsService

@Configuration
@ConditionalOnSarEnabled
class SubjectAccessRequestConfiguration {

  @Bean
  fun prisonerDetailsService(
    courtCaseSarRepository: CourtCaseSarRepository,
    recallSarRepository: RecallSarRepository,
    immigrationDetentionSarRepository: ImmigrationDetentionSarRepository,
    personService: PersonService,
    courtRegisterService: CourtRegisterService,
  ): PrisonerDetailsService = PrisonerDetailsService(
    courtCaseSarRepository,
    recallSarRepository,
    immigrationDetentionSarRepository,
    personService,
    courtRegisterService,
  )
}
