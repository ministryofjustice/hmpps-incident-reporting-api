package uk.gov.justice.digital.hmpps.incidentreporting.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.incidentreporting.dto.DescriptionAddendum
import uk.gov.justice.digital.hmpps.incidentreporting.dto.request.AddDescriptionAddendum
import uk.gov.justice.digital.hmpps.incidentreporting.dto.request.UpdateDescriptionAddendum
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.ReportRepository
import java.time.LocalDateTime
import java.util.UUID
import uk.gov.justice.digital.hmpps.incidentreporting.dto.ReportBasic as ReportBasicDto
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.Report as ReportEntity

@Service
class DescriptionAddendumService(
  reportRepository: ReportRepository,
) : RelatedObjectService<DescriptionAddendum, AddDescriptionAddendum, UpdateDescriptionAddendum>(reportRepository) {
  override fun ReportEntity.toRelatedObjectDtos(): List<DescriptionAddendum> = descriptionAddendums.map { it.toDto() }

  @Transactional
  override fun addObject(
    reportId: UUID,
    request: AddDescriptionAddendum,
    now: LocalDateTime,
    requestUsername: String,
  ): Pair<ReportBasicDto, List<DescriptionAddendum>> {
    return changeReportOrThrowNotFound(reportId, now, requestUsername) { report ->
      val sequence = if (report.descriptionAddendums.isEmpty()) 0 else report.descriptionAddendums.last().sequence + 1
      report.addDescriptionAddendum(
        sequence = sequence,
        createdBy = request.createdBy ?: requestUsername,
        createdAt = request.createdAt ?: now,
        firstName = request.firstName,
        lastName = request.lastName,
        text = request.text,
      )
    }
  }

  @Transactional
  override fun updateObject(
    reportId: UUID,
    index: Int,
    request: UpdateDescriptionAddendum,
    now: LocalDateTime,
    requestUsername: String,
  ): Pair<ReportBasicDto, List<DescriptionAddendum>> {
    return changeReportOrThrowNotFound(reportId, now, requestUsername) { report ->
      report.findDescriptionAddendumByIndex(index).updateWith(request = request, now = now)
    }
  }

  @Transactional
  override fun deleteObject(
    reportId: UUID,
    index: Int,
    now: LocalDateTime,
    requestUsername: String,
  ): Pair<ReportBasicDto, List<DescriptionAddendum>> {
    return changeReportOrThrowNotFound(reportId, now, requestUsername) { report ->
      report.findDescriptionAddendumByIndex(index).let { report.removeDescriptionAddendum(it) }
    }
  }
}
