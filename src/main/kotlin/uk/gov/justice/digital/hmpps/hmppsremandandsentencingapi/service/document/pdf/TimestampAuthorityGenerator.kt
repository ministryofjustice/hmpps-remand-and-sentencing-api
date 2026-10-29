package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document.pdf

import org.bouncycastle.tsp.TSPAlgorithms
import org.bouncycastle.tsp.TimeStampRequestGenerator
import org.bouncycastle.tsp.TimeStampResponse
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import java.math.BigInteger
import java.security.SecureRandom
import java.time.Duration

class TimestampAuthorityGenerator(
  private val webClient: WebClient,
  private val timeout: Duration = Duration.ofSeconds(10),
) {

  /**
   * Generates a Timestamp off the back of the sha256 digest
   * Note: Only a sha256 digest of the signature goes out.
   * No document content or PII is sent to the external api.
   */
  fun generateTimestamp(sha256Digest: ByteArray): ByteArray {
    val request = TimeStampRequestGenerator().apply { setCertReq(true) }
      .generate(TSPAlgorithms.SHA256, sha256Digest, BigInteger(64, SecureRandom()))

    val (status, body) = checkNotNull(
      webClient.post()
        .contentType(MediaType.parseMediaType("application/timestamp-query"))
        .bodyValue(request.encoded)
        .exchangeToMono { response ->
          response.bodyToMono<ByteArray>()
            .defaultIfEmpty(ByteArray(0))
            .map { response.statusCode().value() to it }
        }
        .block(timeout),
    ) { "Timestamp authority returned no response" }
    check(status == 200) { "Timestamp authority returned HTTP $status" }

    val response = TimeStampResponse(body)
    response.validate(request)
    val token = checkNotNull(response.timeStampToken) { "Timestamp authority returned no token: ${response.statusString}" }
    return token.encoded
  }
}
