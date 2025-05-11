package uk.gov.justice.digital.hmpps.incidentreporting.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.incidentreporting.dto.PrisonerInvolvement
import uk.gov.justice.digital.hmpps.incidentreporting.dto.request.AddPrisonerInvolvement
import uk.gov.justice.digital.hmpps.incidentreporting.dto.request.UpdatePrisonerInvolvement
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.ReportRepository
import java.time.LocalDateTime
import java.util.UUID
import uk.gov.justice.digital.hmpps.incidentreporting.dto.ReportBasic as ReportBasicDto
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.Report as ReportEntity

@Service
class PrisonerInvolvementService(
  reportRepository: ReportRepository,
) : RelatedObjectService<PrisonerInvolvement, AddPrisonerInvolvement, UpdatePrisonerInvolvement>(reportRepository) {
  override fun ReportEntity.toRelatedObjectDtos(): List<PrisonerInvolvement> = prisonersInvolved.map { it.toDto() }

  @Transactional
  override fun addObject(
    reportId: UUID,
    request: AddPrisonerInvolvement,
    now: LocalDateTime,
    requestUsername: String,
  ): Pair<ReportBasicDto, List<PrisonerInvolvement>> {
    return changeReportOrThrowNotFound(reportId, now, requestUsername) { report ->
      val sequence = if (report.prisonersInvolved.isEmpty()) 0 else report.prisonersInvolved.last().sequence + 1
      report.addPrisonerInvolved(
        sequence = sequence,
        prisonerNumber = request.prisonerNumber,
        firstName = request.firstName,
        lastName = request.lastName,
        prisonerRole = request.prisonerRole,
        outcome = request.outcome,
        comment = request.comment,
      )
      report.prisonerInvolvementDone = true
    }
  }

  @Transactional
  override fun updateObject(
    reportId: UUID,
    index: Int,
    request: UpdatePrisonerInvolvement,
    now: LocalDateTime,
    requestUsername: String,
  ): Pair<ReportBasicDto, List<PrisonerInvolvement>> {
    return changeReportOrThrowNotFound(reportId, now, requestUsername) { report ->
      report.findPrisonerInvolvedByIndex(index).updateWith(request)
    }
  }

  @Transactional
  override fun deleteObject(
    reportId: UUID,
    index: Int,
    now: LocalDateTime,
    requestUsername: String,
  ): Pair<ReportBasicDto, List<PrisonerInvolvement>> {
    return changeReportOrThrowNotFound(reportId, now, requestUsername) { report ->
      report.findPrisonerInvolvedByIndex(index).let { report.removePrisonerInvolved(it) }
    }
  }
}
