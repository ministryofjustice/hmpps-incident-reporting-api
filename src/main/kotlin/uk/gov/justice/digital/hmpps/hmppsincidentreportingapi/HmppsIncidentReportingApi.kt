package uk.gov.justice.digital.hmpps.hmppsincidentreportingapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication()
class HmppsIncidentReportingApi

fun main(args: Array<String>) {
  runApplication<HmppsIncidentReportingApi>(*args)
}
