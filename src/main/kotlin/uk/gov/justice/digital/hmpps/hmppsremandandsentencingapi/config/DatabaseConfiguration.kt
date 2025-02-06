package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.hibernate.cfg.AvailableSettings
import org.hibernate.type.format.jackson.JacksonJsonFormatMapper
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class DatabaseConfiguration {

  @Bean
  fun jsonFormatMapperCustomizer(objectMapper: ObjectMapper): HibernatePropertiesCustomizer = HibernatePropertiesCustomizer { properties ->
    properties.put(
      AvailableSettings.JSON_FORMAT_MAPPER,
      JacksonJsonFormatMapper(objectMapper),
    )
  }
}
