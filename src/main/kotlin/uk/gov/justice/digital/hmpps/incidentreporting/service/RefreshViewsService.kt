package uk.gov.justice.digital.hmpps.incidentreporting.service

import jakarta.persistence.EntityManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class RefreshViewsService(
  private val entityManager: EntityManager,
  @Value("\${dpr.report.views}") private val materializedViews: List<String>,

) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  /**
   * Refreshes the materialized views in the database.
   *
   * This method executes a native SQL query to refresh the materialized views".
   * It is intended to be called periodically to ensure that the view reflects the latest data.
   */
  fun refreshViews() {
    materializedViews.forEach { materializedView ->
      log.info("Refreshing materialized view $materializedView")
      entityManager.createNativeQuery("REFRESH MATERIALIZED VIEW report.$materializedView").executeUpdate()
      log.info("Materialized view $materializedView refreshed")
    }
  }
}
