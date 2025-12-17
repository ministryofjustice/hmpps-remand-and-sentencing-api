package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.config

import org.springframework.context.annotation.Configuration
import org.springframework.retry.annotation.EnableRetry

@Configuration
@EnableRetry
class RetryConfiguration
