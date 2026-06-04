package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventMetadata
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtCaseEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtCaseRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.subjectaccessrequest.RecallSarRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.subjectaccessrequest.MockedResponseData

@ExtendWith(MockKExtension::class)
class BulkFixManyChargesToSentenceServiceTests {

  @MockK
  private lateinit var courtCaseSarRepository: CourtCaseRepository

  @MockK
  private lateinit var fixManyChargesToSentenceService: FixManyChargesToSentenceService
  
  @MockK
  private lateinit var courtCaseEntity: CourtCaseEntity

  @InjectMockKs(overrideValues = true)
  private lateinit var sut: BulkFixManyChargesToSentenceService

  @Test
  fun `should foo bar`() {

    every { courtCaseSarRepository.findCaseUniqueIdentifierWithManyChargesDataFixByUpdatedAtDesc(10) } returns listOf(
      "70D8677F-3E37-487D-ADA7-EEFE3182438B",
      "B9C71F09-A5C0-4355-B961-BFC229EE7104",
      "28DA01FA-EEC5-4541-890C-F0A0FDD11772",
      "001AE716-4F8E-4C04-B855-2DCCCF5EFA24"
    )

    every { courtCaseSarRepository.findSentencedCourtCase("70D8677F-3E37-487D-ADA7-EEFE3182438B") } returns courtCaseEntity
    every { courtCaseSarRepository.findSentencedCourtCase("B9C71F09-A5C0-4355-B961-BFC229EE7104") } returns courtCaseEntity
    every { fixManyChargesToSentenceService.fixCourtCaseSentences(courtCaseEntity, "BATCH_JOB") } returns 
    
    
    val events = sut.fixCourtCaseSentences(20)

    verify { chargeRepository.save(any()) }
  }

}