package uk.gov.justice.digital.hmpps.incidentreporting.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.PrisonerInvolvementRepository
import uk.gov.justice.hmpps.kotlin.sar.HmppsPrisonSubjectAccessRequestService
import uk.gov.justice.hmpps.kotlin.sar.HmppsSubjectAccessRequestContent
import java.time.LocalDate

@Service
class SubjectAccessRequestService(
  private val prisonerInvolvementRepository: PrisonerInvolvementRepository,
) : HmppsPrisonSubjectAccessRequestService {
  override fun getPrisonContentFor(prn: String, fromDate: LocalDate?, toDate: LocalDate?): HmppsSubjectAccessRequestContent {
    val incidentReports = prisonerInvolvementRepository.findAllByPrisonerNumber(prn)
    val content = incidentReports
      .filter { it.incident.reportedDate.isAfter(fromDate?.atStartOfDay()) && it.incident.reportedDate.isBefore(toDate?.atStartOfDay()) }
      .map {
        it.incident.toDto()
      }
    return HmppsSubjectAccessRequestContent(
      content = content,
    )
  }
}
