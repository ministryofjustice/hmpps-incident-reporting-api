package uk.gov.justice.digital.hmpps.incidentreporting

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

const val SYSTEM_USERNAME = "INCIDENT_REPORTING_API"

@SpringBootApplication
@EnableConfigurationProperties
class HmppsIncidentReportingApi

fun main(args: Array<String>) {
  runApplication<HmppsIncidentReportingApi>(*args)
}
