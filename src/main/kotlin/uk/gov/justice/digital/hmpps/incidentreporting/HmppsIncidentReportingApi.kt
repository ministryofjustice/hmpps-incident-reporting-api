package uk.gov.justice.digital.hmpps.incidentreporting

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

const val SYSTEM_USERNAME = "INCIDENT_REPORTING_API"

@SpringBootApplication
class HmppsIncidentReportingApi

fun main(args: Array<String>) {
  runApplication<HmppsIncidentReportingApi>(*args)
}
