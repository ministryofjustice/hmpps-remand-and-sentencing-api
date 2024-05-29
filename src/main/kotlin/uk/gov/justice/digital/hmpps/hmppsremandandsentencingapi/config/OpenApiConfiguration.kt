package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.servers.Server
import org.springdoc.core.utils.SpringDocUtils
import org.springframework.boot.info.BuildProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Configuration
class OpenApiConfiguration(buildProperties: BuildProperties) {
  private val version: String = buildProperties.version

  @Bean
  fun customOpenAPI(): OpenAPI = OpenAPI()
    .servers(
      listOf(
        Server().url("https://remand-and-sentencing-api.hmpps.service.justice.gov.uk").description("Prod"),
        Server().url("https://remand-and-sentencing-api-preprod.hmpps.service.justice.gov.ukk").description("Preprod"),
        Server().url("https://remand-and-sentencing-api-dev.hmpps.service.justice.gov.uk").description("Development"),
        Server().url("http://localhost:8080").description("Local"),
      ),
    )
    .info(
      Info().title("HMPPS Remand and Sentencing API")
        .version(version)
        .description("Recording Remand and sentencing")
        .contact(Contact().name("HMPPS Digital Studio").email("feedback@digital.justice.gov.uk")),
    )
  init {
    val schema: Schema<LocalTime> = Schema<LocalTime>()
    schema.example(LocalTime.now().format(DateTimeFormatter.ISO_TIME))
    schema.type("string")
    SpringDocUtils.getConfig().replaceWithSchema(LocalTime::class.java, schema)
  }
}
