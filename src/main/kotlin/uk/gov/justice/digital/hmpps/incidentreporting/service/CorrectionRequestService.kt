package uk.gov.justice.digital.hmpps.incidentreporting.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.incidentreporting.dto.CorrectionRequest
import uk.gov.justice.digital.hmpps.incidentreporting.dto.request.AddCorrectionRequest
import uk.gov.justice.digital.hmpps.incidentreporting.dto.request.UpdateCorrectionRequest
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.ReportRepository
import java.time.LocalDateTime
import java.util.UUID
import uk.gov.justice.digital.hmpps.incidentreporting.dto.ReportBasic as ReportBasicDto
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.Report as ReportEntity

@Service
class CorrectionRequestService(
  reportRepository: ReportRepository,
) : RelatedObjectService<CorrectionRequest, AddCorrectionRequest, UpdateCorrectionRequest>(reportRepository) {
  override fun ReportEntity.toRelatedObjectDtos(): List<CorrectionRequest> = correctionRequests.map { it.toDto() }

  @Transactional
  override fun addObject(
    reportId: UUID,
    request: AddCorrectionRequest,
    now: LocalDateTime,
    requestUsername: String,
  ): Pair<ReportBasicDto, List<CorrectionRequest>> {
    return changeReportOrThrowNotFound(reportId, now, requestUsername) { report ->
      val sequence = if (report.correctionRequests.isEmpty()) 0 else report.correctionRequests.last().sequence + 1
      report.addCorrectionRequest(
        sequence = sequence,
        descriptionOfChange = request.descriptionOfChange,
        correctionRequestedBy = requestUsername,
        correctionRequestedAt = now,
        location = request.location,
      )
    }
  }

  @Transactional
  override fun updateObject(
    reportId: UUID,
    index: Int,
    request: UpdateCorrectionRequest,
    now: LocalDateTime,
    requestUsername: String,
  ): Pair<ReportBasicDto, List<CorrectionRequest>> {
    return changeReportOrThrowNotFound(reportId, now, requestUsername) { report ->
      report.findCorrectionRequestByIndex(index).updateWith(
        request = request,
        now = now,
        requestUsername = requestUsername,
      )
    }
  }

  @Transactional
  override fun deleteObject(
    reportId: UUID,
    index: Int,
    now: LocalDateTime,
    requestUsername: String,
  ): Pair<ReportBasicDto, List<CorrectionRequest>> {
    return changeReportOrThrowNotFound(reportId, now, requestUsername) { report ->
      report.findCorrectionRequestByIndex(index).let { report.removeCorrectionRequest(it) }
    }
  }
}
