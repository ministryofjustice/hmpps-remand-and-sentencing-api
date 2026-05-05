package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.ConstructorBinding

@ConfigurationProperties(prefix = "features")
data class FeaturesConfig @ConstructorBinding constructor(
  val appeals: AppealsConfig,
  val markImmigrationCasesInactive: MarkImmigrationCasesInactiveConfig,
)

data class AppealsConfig(
  val enabled: Boolean,
)

data class MarkImmigrationCasesInactiveConfig(
  val enabled: Boolean,
)
