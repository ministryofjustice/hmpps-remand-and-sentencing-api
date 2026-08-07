package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.ConstructorBinding

@ConfigurationProperties(prefix = "features")
data class FeaturesConfig @ConstructorBinding constructor(
  val hmctsWarrantThingToDo: HmctsWarrantThingToDoConfig,
  val appearanceSchedulesEvents: AppearanceSchedulesEventsConfig,
)

data class HmctsWarrantThingToDoConfig(
  val enabled: Boolean,
  val repeatRemandHearingEnabled: Boolean,
)

data class AppearanceSchedulesEventsConfig(
  val enabled: Boolean,
)
