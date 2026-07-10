package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.requests.documentManagementApi

fun documentMetadataRequest(status: String) = """
  {
     "status":"$status"
  }
""".trimIndent()
