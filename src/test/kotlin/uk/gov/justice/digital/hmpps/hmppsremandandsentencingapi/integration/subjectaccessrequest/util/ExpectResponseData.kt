package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.subjectaccessrequest.util

object ExpectResponseData {

  fun emptyNotInNomisResponse(): String =
    """
         {
          "attachments": [],
          "content": {
            "prisonerNumber": "PRI123",
            "immigrationDetentions": []
          }
        }
    """.trimIndent()
}
