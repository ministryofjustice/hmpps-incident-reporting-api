package uk.gov.justice.digital.hmpps.incidentreporting.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.incidentreporting.dto.StaffInvolvement
import uk.gov.justice.digital.hmpps.incidentreporting.dto.request.AddStaffInvolvement
import uk.gov.justice.digital.hmpps.incidentreporting.dto.request.UpdateStaffInvolvement
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.ReportRepository
import java.time.LocalDateTime
import java.util.UUID
import uk.gov.justice.digital.hmpps.incidentreporting.dto.ReportBasic as ReportBasicDto
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.Report as ReportEntity

@Service
class StaffInvolvementService(
  reportRepository: ReportRepository,
) : RelatedObjectService<StaffInvolvement, AddStaffInvolvement, UpdateStaffInvolvement>(reportRepository) {
  override fun ReportEntity.toRelatedObjectDtos(): List<StaffInvolvement> = staffInvolved.map { it.toDto() }

  @Transactional
  override fun addObject(
    reportId: UUID,
    request: AddStaffInvolvement,
    now: LocalDateTime,
    requestUsername: String,
  ): Pair<ReportBasicDto, List<StaffInvolvement>> {
    return changeReportOrThrowNotFound(reportId, now, requestUsername) { report ->
      val sequence = if (report.staffInvolved.isEmpty()) 0 else report.staffInvolved.last().sequence + 1
      report.addStaffInvolved(
        sequence = sequence,
        staffUsername = request.staffUsername,
        firstName = request.firstName,
        lastName = request.lastName,
        staffRole = request.staffRole,
        comment = request.comment,
      )
      report.staffInvolvementDone = true
    }
  }

  @Transactional
  override fun updateObject(
    reportId: UUID,
    index: Int,
    request: UpdateStaffInvolvement,
    now: LocalDateTime,
    requestUsername: String,
  ): Pair<ReportBasicDto, List<StaffInvolvement>> {
    return changeReportOrThrowNotFound(reportId, now, requestUsername) { report ->
      report.findStaffInvolvedByIndex(index).updateWith(request)
    }
  }

  @Transactional
  override fun deleteObject(
    reportId: UUID,
    index: Int,
    now: LocalDateTime,
    requestUsername: String,
  ): Pair<ReportBasicDto, List<StaffInvolvement>> {
    return changeReportOrThrowNotFound(reportId, now, requestUsername) { report ->
      report.findStaffInvolvedByIndex(index).let { report.removeStaffInvolved(it) }
    }
  }
}
