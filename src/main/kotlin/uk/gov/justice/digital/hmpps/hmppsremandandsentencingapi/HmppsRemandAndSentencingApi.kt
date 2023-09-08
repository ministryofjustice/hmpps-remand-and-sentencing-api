package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication()
class HmppsRemandAndSentencingApi

fun main(args: Array<String>) {
  runApplication<HmppsRemandAndSentencingApi>(*args)
}
