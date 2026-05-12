package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.config

import org.springframework.context.annotation.Condition
import org.springframework.context.annotation.ConditionContext
import org.springframework.context.annotation.Conditional
import org.springframework.core.type.AnnotatedTypeMetadata

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Conditional(OnSarEnabledCondition::class)
annotation class ConditionalOnSarEnabled

internal class OnSarEnabledCondition : Condition {
  override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata): Boolean = "true".equals(context.environment.getProperty("hmpps.sar.enabled"), ignoreCase = true)
}
