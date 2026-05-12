package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.subjectaccessrequest.alldata.util

object ExpectResponseData {

  fun emptyFullDataResponse(): String = """
        {
            "content": {
                "prisonerNumber": "PRI123",
                "prisonerName": "Cormac Meza",
                "courtCases": [],
                "recalls": [],
                "immigrationDetentions": []
            },
            "attachments": []
        }
  """.trimIndent()
}
