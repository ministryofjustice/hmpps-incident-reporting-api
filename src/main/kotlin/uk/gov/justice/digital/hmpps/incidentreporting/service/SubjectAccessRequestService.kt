package uk.gov.justice.digital.hmpps.incidentreporting.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.PrisonerInvolvementRepository
import uk.gov.justice.hmpps.kotlin.sar.HmppsPrisonSubjectAccessRequestService
import uk.gov.justice.hmpps.kotlin.sar.HmppsSubjectAccessRequestContent
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class SubjectAccessRequestService(
  private val prisonerInvolvementRepository: PrisonerInvolvementRepository,
) : HmppsPrisonSubjectAccessRequestService {
  override fun getPrisonContentFor(prn: String, fromDate: LocalDate?, toDate: LocalDate?): HmppsSubjectAccessRequestContent {
    val incidentReports = prisonerInvolvementRepository.findAllByPrisonerNumber(prn)
    val content = incidentReports
      .filter { it.incident.reportedDate.isAfter(fromDate?.atStartOfDay()) && it.incident.reportedDate.isBefore(toDate?.atStartOfDay()) }
      .map {
        IncidentDTO(
          prisonerNumber = it.prisonerNumber,
          incidentNumber = it.incident.incidentNumber,
          reportedDate = it.incident.reportedDate,
        )
      }
    return HmppsSubjectAccessRequestContent(
      content = content,
    )
  }
}

data class IncidentDTO(
  val prisonerNumber: String,
  val incidentNumber: String,
  val reportedDate: LocalDateTime,
)
