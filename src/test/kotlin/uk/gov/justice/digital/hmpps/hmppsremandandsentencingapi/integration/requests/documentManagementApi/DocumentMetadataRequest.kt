package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.requests.documentManagementApi

fun documentMetadataRequest(prisonerId: String, status: String) = """
  {
     "prisonerId": "$prisonerId",
     "source":"RemandSentencingUser",
     "status":"$status"
  }
""".trimIndent()
