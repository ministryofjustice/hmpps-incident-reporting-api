package uk.gov.justice.digital.hmpps.incidentreporting.resource

import com.microsoft.applicationinsights.TelemetryClient
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.RequestMapping
import uk.gov.justice.digital.hmpps.incidentreporting.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.incidentreporting.config.trackEvent
import uk.gov.justice.digital.hmpps.incidentreporting.constants.InformationSource
import uk.gov.justice.digital.hmpps.incidentreporting.dto.ReportBasic
import uk.gov.justice.digital.hmpps.incidentreporting.service.ReportDomainEventType
import uk.gov.justice.digital.hmpps.incidentreporting.service.ReportService.Companion.log
import uk.gov.justice.digital.hmpps.incidentreporting.service.WhatChanged
import uk.gov.justice.hmpps.kotlin.auth.HmppsAuthenticationHolder
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

@RequestMapping("/incident-reports/{reportId}", produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(
  name = "Objects related to incident reports",
  description = "Create, retrieve, update and delete objects that are related to incident reports",
)
abstract class ReportRelatedObjectResource<ResponseDto, AddRequest, UpdateRequest> : EventBaseResource() {
  @Autowired
  private lateinit var clock: Clock

  @Autowired
  private lateinit var authenticationHolder: HmppsAuthenticationHolder

  @Autowired
  private lateinit var telemetryClient: TelemetryClient

  protected fun publishChangeEvents(
    changeMessage: String,
    changeReport: (LocalDateTime, String) -> Pair<ReportBasic, List<ResponseDto>>,
  ): List<ResponseDto> {
    val now = LocalDateTime.now(clock)
    val requestUsername = authenticationHolder.username ?: SYSTEM_USERNAME

    return changeReport(now, requestUsername).let { (basicReport, relatedObjects) ->
      eventPublishAndAudit(
        event = ReportDomainEventType.INCIDENT_REPORT_AMENDED,
        informationSource = InformationSource.DPS,
        whatChanged = whatChanges,
      ) {
        basicReport
      }

      log.info("$changeMessage reference=${basicReport.reportReference} ID=${basicReport.id}")
      telemetryClient.trackEvent(
        changeMessage,
        basicReport,
      )

      relatedObjects
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
