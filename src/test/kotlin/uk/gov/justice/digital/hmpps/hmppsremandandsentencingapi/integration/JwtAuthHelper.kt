package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.Jwts.SIG
import org.springframework.context.annotation.Bean
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.stereotype.Component
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPublicKey
import java.time.Duration
import java.util.Date
import java.util.UUID

@Component
class JwtAuthHelper {
  private val keyPair: KeyPair

  init {
    val gen = KeyPairGenerator.getInstance("RSA")
    gen.initialize(2048)
    keyPair = gen.generateKeyPair()
  }

  @Bean
  fun jwtDecoder(): JwtDecoder = NimbusJwtDecoder.withPublicKey(keyPair.public as RSAPublicKey).build()

  fun createJwt(
    subject: String,
    user: String?,
    client: String?,
    scope: List<String>? = listOf(),
    roles: List<String>? = listOf(),
    expiryTime: Duration = Duration.ofHours(1),
    jwtId: String = UUID.randomUUID().toString(),
  ): String = mutableMapOf<String, Any>()
    .also { user?.let { user -> it["user_name"] = user } }
    .also { client?.let { client -> it["client_id"] = client } }
    .also { roles?.let { roles -> it["authorities"] = roles } }
    .also { scope?.let { scope -> it["scope"] = scope } }
    .let {
      Jwts.builder()
        .id(jwtId)
        .subject(subject)
        .claims(it.toMap())
        .expiration(Date(System.currentTimeMillis() + expiryTime.toMillis()))
        .signWith(keyPair.private, SIG.RS256)
        .compact()
    }
}
