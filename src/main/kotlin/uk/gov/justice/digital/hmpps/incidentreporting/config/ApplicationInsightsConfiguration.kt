package uk.gov.justice.digital.hmpps.incidentreporting.config

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import uk.gov.justice.digital.hmpps.incidentreporting.dto.ReportBasic

/**
 * TelemetryClient gets altered at runtime by the java agent and so is a no-op otherwise
 */
@Configuration
class ApplicationInsightsConfiguration {
  @Bean
  fun telemetryClient(): TelemetryClient = TelemetryClient()
}

fun TelemetryClient.trackEvent(name: String, properties: Map<String, String>) {
  trackEvent(name, properties, null)
}

fun TelemetryClient.trackEvent(name: String, report: ReportBasic, extraProperties: Map<String, String>? = null) {
  val properties = mutableMapOf(
    "id" to report.id.toString(),
    "incidentNumber" to report.incidentNumber,
    "prisonId" to report.prisonId,
  )
  extraProperties?.let { properties.putAll(it) }
  trackEvent(name, properties, null)
}
