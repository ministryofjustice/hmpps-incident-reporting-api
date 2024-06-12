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
    val prisonerInvolvementList = prisonerInvolvementRepository.findAllByPrisonerNumber(prn)
    val content = prisonerInvolvementList
      .asSequence()
      .map { it.getReport() }
      .distinctBy { it.id }
      .filter { it.incidentDateAndTime.isAfter(fromDate?.atStartOfDay()) && it.incidentDateAndTime.isBefore(toDate?.atStartOfDay()) }
      .map { it.toDtoWithDetails() }
      .sortedBy { it.incidentDateAndTime }
      .toList()
    return HmppsSubjectAccessRequestContent(
      content = content,
    )
  }
}
