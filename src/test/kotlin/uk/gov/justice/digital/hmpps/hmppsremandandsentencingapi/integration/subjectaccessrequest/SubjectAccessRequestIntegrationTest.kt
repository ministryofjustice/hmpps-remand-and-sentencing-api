package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.subjectaccessrequest

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ImmigrationDetentionNoLongerOfInterestType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ImmigrationDetentionRecordType.IS91
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarApiDataTest
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarIntegrationTestHelper
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarIntegrationTestHelperConfig
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarReportTest
import java.time.LocalDate

@Import(SarIntegrationTestHelperConfig::class)
@ActiveProfiles("test", "test-sar")
class SubjectAccessRequestIntegrationTest :
  IntegrationTestBase(),
  SarApiDataTest,
  SarReportTest {

  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  lateinit var sarIntegrationTestHelper: SarIntegrationTestHelper

  override fun setupTestData() {
    // Data setup
    val (sentenceOne, _) = createCourtCaseTwoSentences()
    createImmigrationDetention(
      DpsDataCreator.dpsCreateImmigrationDetention(
        prisonerId = DpsDataCreator.DEFAULT_PRISONER_ID,
        immigrationDetentionRecordType = IS91,
        recordDate = LocalDate.of(2021, 1, 1),
        createdByUsername = "aUser",
        createdByPrison = "PRI",
        appearanceOutcomeUuid = IMMIGRATION_IS91_UUID,
        noLongerOfInterestReason = ImmigrationDetentionNoLongerOfInterestType.RIGHT_TO_REMAIN,
        noLongerOfInterestComment = "Currently has the right to Remain",
        homeOfficeReferenceNumber = "3240593452",
      ),
    )

    createImmigrationDetention(
      DpsDataCreator.dpsCreateImmigrationDetention(
        prisonerId = DpsDataCreator.DEFAULT_PRISONER_ID,
        immigrationDetentionRecordType = IS91,
        recordDate = LocalDate.of(2025, 2, 2),
        createdByUsername = "aUser",
        createdByPrison = "PRI",
        appearanceOutcomeUuid = IMMIGRATION_IS91_UUID,
        noLongerOfInterestReason = ImmigrationDetentionNoLongerOfInterestType.BRITISH_CITIZEN,
        noLongerOfInterestComment = "Recently made a British Citizen",
        homeOfficeReferenceNumber = "5340593452",
      ),
    )
  }

  override fun getPrn(): String? = DpsDataCreator.DEFAULT_PRISONER_ID

  override fun getSarHelper(): SarIntegrationTestHelper = sarIntegrationTestHelper

  override fun getWebTestClientInstance(): WebTestClient = webTestClient
}
