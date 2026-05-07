package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.subjectaccessrequest.CourtCaseUnsyncedSarRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.subjectaccessrequest.ImmigrationDetentionUnsyncedSarRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.subjectaccessrequest.alldata.CourtCaseSarRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.subjectaccessrequest.alldata.ImmigrationDetentionSarRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.subjectaccessrequest.alldata.RecallSarRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.CourtRegisterService
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.PersonService
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.subjectaccessrequest.AllPrisonerDetailsService
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.subjectaccessrequest.PrisonerDetailsService
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.subjectaccessrequest.UnsyncedPrisonerDetailsService

@Configuration
@ConditionalOnSarEnabled
class SubjectAccessRequestConfiguration {

  @ConditionalOnProperty(
    prefix = "hmpps.sar.mode.all-sar-data",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = false,
  )
  @Bean
  fun allPrisonerDetailsService(
    courtCaseSarRepository: CourtCaseSarRepository,
    recallSarRepository: RecallSarRepository,
    immigrationDetentionSarRepository: ImmigrationDetentionSarRepository,
    personService: PersonService,
    courtRegisterService: CourtRegisterService,
  ): PrisonerDetailsService = AllPrisonerDetailsService(
    courtCaseSarRepository,
    recallSarRepository,
    immigrationDetentionSarRepository,
    personService,
    courtRegisterService,
  )

  @ConditionalOnProperty(
    prefix = "hmpps.sar.mode.all-sar-data",
    name = ["enabled"],
    havingValue = "false",
    matchIfMissing = true,
  )
  @Bean
  fun unsyncedPrisonerDetailsService(
    immigrationDetentionUnsyncedSarRepository: ImmigrationDetentionUnsyncedSarRepository,
    courtCaseUnsyncedSarRepository: CourtCaseUnsyncedSarRepository,
  ): PrisonerDetailsService = UnsyncedPrisonerDetailsService(immigrationDetentionUnsyncedSarRepository, courtCaseUnsyncedSarRepository)
}
