package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.responses.prisonapi

fun prisonerDetailsResponse() = """
  {
    "offenderNo": "A1234AB",
    "bookingId": "1234",
    "firstName": "Marvin",
    "lastName": "Haggler",
    "dateOfBirth": "1965-02-03",
    "agencyId": "MDI",
    "assignedLivingUnit": {
      "agencyName": "HMP Bedford",
      "description": "CELL-1"
    },
    "identifiers": [
      {
        "type": "PNC",
        "value": "1231/XX/121",
        "offenderNo": "A1234AB",
        "bookingId": 1231223,
        "issuedAuthorityText": "Important Auth",
        "issuedDate": "2018-01-21",
        "caseloadType": "GENERAL",
        "whenCreated": "2021-07-05T10:35:17"
      }
    ],
    "legalStatus": "REMAND"
    }
""".trimIndent()
