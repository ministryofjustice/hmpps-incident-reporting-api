package uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository

import org.springframework.data.repository.CrudRepository
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.AnalyticalMarker
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.AnalyticalMarkerPk

interface AnalysisMarkerRepository : CrudRepository<AnalyticalMarker, AnalyticalMarkerPk> {
  fun findByIdResponseCode(responseCode: String): List<AnalyticalMarker>
}
