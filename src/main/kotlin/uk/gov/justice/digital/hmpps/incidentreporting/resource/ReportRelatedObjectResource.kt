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
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.Report
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.ReportRepository
import uk.gov.justice.digital.hmpps.incidentreporting.service.ReportDomainEventType
import uk.gov.justice.digital.hmpps.incidentreporting.service.ReportService.Companion.log
import uk.gov.justice.digital.hmpps.incidentreporting.service.WhatChanged
import uk.gov.justice.hmpps.kotlin.auth.HmppsAuthenticationHolder
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID
import kotlin.jvm.optionals.getOrNull

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
  private lateinit var reportRepository: ReportRepository

  protected fun (UUID).findReportOrThrowNotFound(): Report {
    return reportRepository.findById(this).getOrNull()
      ?: throw ReportNotFoundException(this)
  }

  protected fun <T> (UUID).updateReportOrThrowNotFound(changeMessage: String, block: (Report) -> T): T {
    val report = findReportOrThrowNotFound()
    return block(report).also {
      report.modifiedIn = InformationSource.DPS
      report.modifiedAt = LocalDateTime.now(clock)
      report.modifiedBy = authenticationHolder.username ?: SYSTEM_USERNAME

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
        // TODO: should different related object actions raise different events?
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
