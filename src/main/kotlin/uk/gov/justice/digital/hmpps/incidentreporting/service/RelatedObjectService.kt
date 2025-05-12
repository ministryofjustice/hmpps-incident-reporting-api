package uk.gov.justice.digital.hmpps.incidentreporting.service

import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.incidentreporting.constants.InformationSource
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.ReportRepository
import uk.gov.justice.digital.hmpps.incidentreporting.resource.ReportNotFoundException
import java.time.LocalDateTime
import java.util.UUID
import kotlin.jvm.optionals.getOrNull
import uk.gov.justice.digital.hmpps.incidentreporting.dto.ReportBasic as ReportBasicDto
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.Report as ReportEntity

abstract class RelatedObjectService<ResponseDto, AddRequest, UpdateRequest>(
  private val reportRepository: ReportRepository,
) {
  private fun findReportOrThrowNotFound(reportId: UUID): ReportEntity {
    return reportRepository.findById(reportId).getOrNull()
      ?: throw ReportNotFoundException(reportId)
  }

  protected fun changeReportOrThrowNotFound(
    reportId: UUID,
    now: LocalDateTime,
    requestUser: String,
    changeReport: (ReportEntity) -> Unit,
  ): Pair<ReportBasicDto, List<ResponseDto>> {
    return findReportOrThrowNotFound(reportId)
      .also(changeReport)
      .let { report ->
        report.modifiedIn = InformationSource.DPS
        report.modifiedAt = now
        report.modifiedBy = requestUser

        report.toDtoBasic() to report.toRelatedObjectDtos()
      }
  }

  protected abstract fun ReportEntity.toRelatedObjectDtos(): List<ResponseDto>

  @Transactional(readOnly = true)
  open fun listObjects(reportId: UUID): List<ResponseDto> {
    return findReportOrThrowNotFound(reportId)
      .toRelatedObjectDtos()
  }

  abstract fun addObject(
    reportId: UUID,
    request: AddRequest,
    now: LocalDateTime,
    requestUsername: String,
  ): Pair<ReportBasicDto, List<ResponseDto>>

  abstract fun updateObject(
    reportId: UUID,
    index: Int,
    request: UpdateRequest,
    now: LocalDateTime,
    requestUsername: String,
  ): Pair<ReportBasicDto, List<ResponseDto>>

  abstract fun deleteObject(
    reportId: UUID,
    index: Int,
    now: LocalDateTime,
    requestUsername: String,
  ): Pair<ReportBasicDto, List<ResponseDto>>
}
