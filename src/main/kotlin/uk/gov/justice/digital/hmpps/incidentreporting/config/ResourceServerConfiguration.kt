package uk.gov.justice.digital.hmpps.incidentreporting.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.CaseloadProvider
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.DefaultDprAuthAwareTokenConverter
import uk.gov.justice.hmpps.kotlin.auth.dsl.ResourceServerConfigurationCustomizer

@Configuration
class ResourceServerConfiguration(
  @Value("\${dpr.endpoint.api.roles}") private val dprEndpointApiRoles: List<String>,
  private val caseloadProvider: CaseloadProvider,
) {
  @Bean
  fun hmppsSecurityFilterChain(
    http: HttpSecurity,
    customizer: ResourceServerConfigurationCustomizer,
  ): SecurityFilterChain = http {
    sessionManagement { SessionCreationPolicy.STATELESS }
    headers { frameOptions { sameOrigin = true } }
    csrf { disable() }
    authorizeHttpRequests {
      customizer.authorizeHttpRequestsCustomizer.dsl
        // override the entire authorizeHttpRequests DSL
        ?.also { dsl -> dsl.invoke(this) }
        // apply specific customizations to the default authorizeHttpRequests DSL
        ?: also {
          customizer.unauthorizedRequestPathsCustomizer.unauthorizedRequestPaths.forEach { authorize(it, permitAll) }
          customizer.anyRequestRoleCustomizer.defaultRole
            ?.also { authorize(anyRequest, hasRole(it)) }
            ?: also {
              authorize("/report/**", hasAnyRole(*dprEndpointApiRoles.toTypedArray()))
              authorize("/reports/**", hasAnyRole(*dprEndpointApiRoles.toTypedArray()))
              authorize("/definitions/**", hasAnyRole(*dprEndpointApiRoles.toTypedArray()))
              authorize("/statements/**", hasAnyRole(*dprEndpointApiRoles.toTypedArray()))
              authorize(anyRequest, authenticated)
            }
        }
    }
    oauth2ResourceServer {
      jwt { jwtAuthenticationConverter = DefaultDprAuthAwareTokenConverter(caseloadProvider) }
    }
  }
    .let { http.build() }

  @Bean
  fun resourceServerCustomizer() = ResourceServerConfigurationCustomizer {
    unauthorizedRequestPaths {
      addPaths = setOf(
        "/queue-admin/retry-all-dlqs",
      )
    }
  }
}
