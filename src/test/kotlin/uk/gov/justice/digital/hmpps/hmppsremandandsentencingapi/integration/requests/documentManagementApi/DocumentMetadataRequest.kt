package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.requests.documentManagementApi

fun documentMetadataRequest(status: String, caseReferences: String): String {
  val caseReferencesList = if (!caseReferences.isEmpty()) {
    """
      ,
      "caseReferences": [ "$caseReferences" ]
    """.trimIndent()
  } else {
    ""
  }

  return """
    {
       "status":"$status",
       "isUnread":false
       $caseReferencesList
    }
  """.trimIndent()
}
