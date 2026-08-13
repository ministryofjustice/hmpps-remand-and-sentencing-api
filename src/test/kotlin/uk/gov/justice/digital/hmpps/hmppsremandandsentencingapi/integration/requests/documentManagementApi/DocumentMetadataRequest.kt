package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.requests.documentManagementApi

fun documentMetadataRequest(status: String, caseReference: String?): String {
  val caseReferencesList = if (!caseReference.isNullOrBlank()) {
    """
      ,
      "caseReferences": [ "$caseReference" ]
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
