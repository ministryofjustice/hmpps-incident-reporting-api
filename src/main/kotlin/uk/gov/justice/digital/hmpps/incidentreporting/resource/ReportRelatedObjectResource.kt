package uk.gov.justice.digital.hmpps.incidentreporting.resource

import com.microsoft.applicationinsights.TelemetryClient
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.RequestMapping
import uk.gov.justice.digital.hmpps.incidentreporting.config.trackEvent
import uk.gov.justice.digital.hmpps.incidentreporting.constants.InformationSource
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.Report
import uk.gov.justice.digital.hmpps.incidentreporting.service.ChangeReportService
import uk.gov.justice.digital.hmpps.incidentreporting.service.ReportDomainEventType
import uk.gov.justice.digital.hmpps.incidentreporting.service.ReportService.Companion.log
import uk.gov.justice.digital.hmpps.incidentreporting.service.WhatChanged
import uk.gov.justice.hmpps.kotlin.auth.HmppsAuthenticationHolder
import java.time.Clock
import java.util.UUID

@RequestMapping("/incident-reports/{reportId}", produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(
  name = "Objects related to incident reports",
  description = "Create, retrieve, update and delete objects that are related to incident reports",
)
abstract class ReportRelatedObjectResource<ResponseDto, AddRequest, UpdateRequest> : EventBaseResource() {
  @Autowired
  protected lateinit var clock: Clock

  @Autowired
  protected lateinit var authenticationHolder: HmppsAuthenticationHolder

  @Autowired
  private lateinit var telemetryClient: TelemetryClient

  @Autowired
  protected lateinit var changeReportService: ChangeReportService

  protected fun publishChangeEvents(changeMessage: String, changeReport: () -> Report): Report {
    return changeReport().also { report ->
      val basicReport = report.toDtoBasic()
      eventPublishAndAudit(
        event = ReportDomainEventType.INCIDENT_REPORT_AMENDED,
        informationSource = InformationSource.DPS,
        whatChanged = whatChanges,
      ) {
        basicReport
      }

      log.info("$changeMessage reference=${report.reportReference} ID=${report.id}")
      telemetryClient.trackEvent(
        changeMessage,
        basicReport,
      )
    }
  }

  protected abstract val whatChanges: WhatChanged

  abstract fun listObjects(reportId: UUID): List<ResponseDto>
  abstract fun addObject(reportId: UUID, @Valid request: AddRequest): List<ResponseDto>
  abstract fun updateObject(
    reportId: UUID,
    index: Int,
    @Valid request: UpdateRequest,
  ): List<ResponseDto>
  abstract fun removeObject(reportId: UUID, index: Int): List<ResponseDto>
}
