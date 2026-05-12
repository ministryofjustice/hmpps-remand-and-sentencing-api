package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.subjectaccessrequest.util

object ExpectResponseData {

  fun validNotInNomisResponse(): String =
    """
       {
          "attachments": [],
          "content": {
            "prisonerNumber": "PRI123",
            "immigrationDetentions": [
              {
                "homeOfficeReferenceNumber": "124222111",
                "noLongerOfInterestReason": "No Data Held",
                "noLongerOfInterestComment": "No Data Held"
              },
              {
                "homeOfficeReferenceNumber": "No Data Held",
                "noLongerOfInterestReason": "RIGHT_TO_REMAIN",
                "noLongerOfInterestComment": ""
              }
            ]
          }
        }
    """.trimIndent()

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
