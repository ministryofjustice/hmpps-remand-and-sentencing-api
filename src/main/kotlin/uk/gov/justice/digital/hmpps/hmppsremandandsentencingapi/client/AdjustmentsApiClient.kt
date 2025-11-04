package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.AdjustmentDto
import java.util.UUID

@Component
class AdjustmentsApiClient(@Qualifier("adjustmentsApiWebClient") private val webClient: WebClient) {

  fun getRecallAdjustment(prisonerId: String, recallUuid: UUID): AdjustmentDto? = webClient
    .get()
    .uri { builder ->
      builder.path("/adjustments")
      builder.queryParam("person", prisonerId)
      builder.queryParam("recallId", recallUuid.toString())
      builder.build()
    }
    .retrieve()
    .bodyToMono(typeReference<List<AdjustmentDto>>())
    .block()!!
    .apply { require(size <= 1) { "Received more than one adjustment for a recall. Should be impossible." } }
    .firstOrNull()

  fun deleteAdjustment(adjustmentId: String) {
    webClient
      .delete()
      .uri("/adjustments/$adjustmentId")
      .retrieve()
      .toBodilessEntity()
      .block()
  }
}
