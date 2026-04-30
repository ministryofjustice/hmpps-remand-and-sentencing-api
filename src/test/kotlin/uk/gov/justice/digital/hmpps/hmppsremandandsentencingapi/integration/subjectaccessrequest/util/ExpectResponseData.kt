package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.subjectaccessrequest.util

object ExpectResponseData {

  fun validNotInNomisResponse(): String =
    """
       {
          "attachments": [],
          "content": {
            "prisonerNumber": "A6764DZ",
            "prisonerName": "RALPH DOG",
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
            "prisonerNumber": "foo-bar",
            "prisonerName": "No Data Held",
            "immigrationDetentions": []
          }
        }
    """.trimIndent()
}
