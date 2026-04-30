package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.subjectaccessrequest.alldata.util

object ExpectResponseData {

  fun validFullDataResponse(): String =
    """
      {
          "attachments": [],
          "content": {
            "prisonerNumber": "A6764DZ",
            "prisonerName": "RALPH DOG",
            "courtCases": [
              {
                "courtName": "Glasgow High Court",
                "caseStatus": "ACTIVE",
                "createdAt": "2026-02-03 10:02",
                "updatedAt": "2026-02-03 10:02",
                "latestCourtAppearance": {
                  "appearanceDate": "2026-02-03",
                  "appearanceOutcomeName": "Imprisonment",
                  "warrantType": "SENTENCING",
                  "convictionDate": "No Data Held",
                  "nextAppearanceDate": "No Data Held",
                  "charges": [
                    {
                      "offenceCode": "RF96124",
                      "offenceDescription": "A person procuring or persuading, or attempting to procure or persuade a reserve force naval rating or marine, liable",
                      "terrorRelated": "No",
                      "foreignPowerRelated": "No",
                      "domesticViolenceRelated": "No",
                      "offenceStartDate": "1997-01-01",
                      "offenceEndDate": "No Data Held",
                      "chargeOutcome": "Imprisonment",
                      "sentences": [
                        {
                          "sentenceType": "ORA Breach Top Up Supervision",
                          "sentenceServeType": "BOTUS",
                          "periods": [
                            {
                              "years": 0,
                              "months": 6,
                              "weeks": 0,
                              "days": 0
                            }
                          ],
                          "periodOrder": "CONCURRENT",
                          "isRecallable": "No"
                        }
                      ]
                    }
                  ]
                }
              }              
            ],
            "recalls": [
              {
                "recallType": "LR",
                "revocationDate": "2025-06-10",
                "returnToCustodyDate": "No Data Held",
                "inPrisonOnRevocationDate": "Yes",
                "recallSentenceStatus": "DELETED"
              },
              {
                "recallType": "CUR_HDC",
                "revocationDate": "No Data Held",
                "returnToCustodyDate": "No Data Held",
                "inPrisonOnRevocationDate": "No",
                "recallSentenceStatus": "ACTIVE"
              },
              {
                "recallType": "LR",
                "revocationDate": "2026-03-09",
                "returnToCustodyDate": "No Data Held",
                "inPrisonOnRevocationDate": "Yes",
                "recallSentenceStatus": "ACTIVE"
              }
            ],
            "immigrationDetentions": [
              {
                "immigrationDetentionRecordType": "DEPORTATION_ORDER",
                "homeOfficeReferenceNumber": "124222111",
                "recordDate": "2026-02-09",
                "noLongerOfInterestReason": "No Data Held",
                "noLongerOfInterestComment": "No Data Held"
              },
              {
                "immigrationDetentionRecordType": "NO_LONGER_OF_INTEREST",
                "homeOfficeReferenceNumber": "No Data Held",
                "recordDate": "2026-03-01",
                "noLongerOfInterestReason": "RIGHT_TO_REMAIN",
                "noLongerOfInterestComment": ""
              }
            ]
          }
        }
    """.trimIndent()

  fun emptyFullDataResponse(): String = """
        {
          "attachments": [],
          "content": {
            "prisonerNumber": "foo-bar",
            "prisonerName": "No Data Held",
            "courtCases": [],
            "recalls": [],
            "immigrationDetentions": []
          }
        }
  """.trimIndent()
}
