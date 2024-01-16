package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.requests.documentManagementApi

fun documentMetadataRequest(prisonerId: String) = """
  {
     "prisonerId": "$prisonerId",
     "state":"COMPLETE"
  }
""".trimIndent()
