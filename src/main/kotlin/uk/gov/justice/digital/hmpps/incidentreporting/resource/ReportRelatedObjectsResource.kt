package uk.gov.justice.digital.hmpps.incidentreporting.resource

import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import uk.gov.justice.digital.hmpps.incidentreporting.config.AuthenticationFacade
import uk.gov.justice.digital.hmpps.incidentreporting.constants.InformationSource
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.Report
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.ReportRepository
import uk.gov.justice.digital.hmpps.incidentreporting.service.ReportDomainEventType
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID
import kotlin.jvm.optionals.getOrNull

@RequestMapping("/incident-reports/{reportId}", produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(
  name = "Objects related to incident reports",
  description = "Create, retrieve, update and delete objects that are related to incident reports",
)
abstract class ReportRelatedObjectsResource<ResponseDto, AddRequest, UpdateRequest> : EventBaseResource() {
  @Autowired
  protected lateinit var clock: Clock

  @Autowired
  private lateinit var authenticationFacade: AuthenticationFacade

  @Autowired
  private lateinit var reportRepository: ReportRepository

  protected fun (UUID).findReportOrThrowNotFound(): Report {
    return reportRepository.findById(this).getOrNull()
      ?: throw ReportNotFoundException(this)
  }

  protected fun <T> (UUID).updateReportOrThrowNotFound(block: (Report) -> T): T {
    val report = findReportOrThrowNotFound()
    return block(report).also {
      report.modifiedAt = LocalDateTime.now(clock)
      report.modifiedBy = authenticationFacade.getUserOrSystemInContext()
      eventPublishAndAudit(
        // TODO: should different related object actions raise different events?
        ReportDomainEventType.INCIDENT_REPORT_AMENDED,
        InformationSource.DPS,
      ) {
        report.toDtoBasic()
      }
    }
  }

  protected inline fun <reified T : Any> List<T>.elementAtIndex(index: Int): T {
    if (index < 1 || index > size) {
      throw ObjectAtIndexNotFoundException(T::class, index)
    }
    return get(index - 1)
  }

  protected inline fun <reified T : Any> MutableList<T>.removeElementAtIndex(index: Int) {
    if (index < 1 || index > size) {
      throw ObjectAtIndexNotFoundException(T::class, index)
    }
    removeAt(index - 1)
  }

  abstract fun listObjects(@PathVariable reportId: UUID): List<ResponseDto>
  abstract fun addObject(@PathVariable reportId: UUID, @Valid request: AddRequest): List<ResponseDto>
  abstract fun updateObject(@PathVariable reportId: UUID, index: Int, @Valid request: UpdateRequest): List<ResponseDto>
  abstract fun removeObject(@PathVariable reportId: UUID, index: Int): List<ResponseDto>
}
