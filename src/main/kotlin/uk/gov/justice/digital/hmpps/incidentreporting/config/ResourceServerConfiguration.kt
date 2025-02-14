package uk.gov.justice.digital.hmpps.incidentreporting.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import uk.gov.justice.hmpps.kotlin.auth.AuthAwareTokenConverter
import uk.gov.justice.hmpps.kotlin.auth.dsl.ResourceServerConfigurationCustomizer

@Configuration
class ResourceServerConfiguration(
  @Value("\${dpr.endpoint.api.role}") private var dprEndpointApiRole: String,
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
              // TODO: This has to be a user role - not ideal but will secure with this for now
              authorize("/report/**", hasRole(dprEndpointApiRole))
              authorize("/reports/**", hasRole(dprEndpointApiRole))
              authorize("/definitions/**", hasRole(dprEndpointApiRole))
              authorize("/statements/**", hasRole(dprEndpointApiRole))
              authorize(anyRequest, authenticated)
            }
        }
    }
    oauth2ResourceServer {
      // TODO: will allow override of the DprAuthAwareTokenConverter
      jwt { jwtAuthenticationConverter = AuthAwareTokenConverter() }
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
