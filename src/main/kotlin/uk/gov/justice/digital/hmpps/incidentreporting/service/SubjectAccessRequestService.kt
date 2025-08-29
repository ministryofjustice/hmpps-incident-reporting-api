package uk.gov.justice.digital.hmpps.incidentreporting.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.incidentreporting.dto.ReportWithDetails
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.PrisonerInvolvementRepository
import uk.gov.justice.digital.hmpps.incidentreporting.resource.SubjectAccessRequestNoReports
import uk.gov.justice.hmpps.kotlin.sar.HmppsPrisonSubjectAccessRequestService
import uk.gov.justice.hmpps.kotlin.sar.HmppsSubjectAccessRequestContent
import java.time.LocalDate

@Service
@Transactional(readOnly = true)
class SubjectAccessRequestService(
  private val prisonerInvolvementRepository: PrisonerInvolvementRepository,
) : HmppsPrisonSubjectAccessRequestService {
  override fun getPrisonContentFor(
    prn: String,
    fromDate: LocalDate?,
    toDate: LocalDate?,
  ): HmppsSubjectAccessRequestContent {
    // date range is supposed to select data that was modified, inclusive of both ends
    val fromDateExclusive = fromDate?.minusDays(1)
    val toDateExclusive = toDate?.plusDays(1)

    val prisonerInvolvementList = prisonerInvolvementRepository.findAllByPrisonerNumber(prn)
    val content: List<ReportWithDetails> = prisonerInvolvementList
      .asSequence()
      .map { it.report }
      .distinctBy { it.id }
      .filter {
        val modifiedDate = it.modifiedAt.toLocalDate()
        (fromDateExclusive == null || modifiedDate.isAfter(fromDateExclusive)) &&
          (toDateExclusive == null || modifiedDate.isBefore(toDateExclusive))
      }
      .map { it.toDtoWithDetails(includeHistory = true) }
      .sortedBy { it.incidentDateAndTime }
      .toList()

    if (content.isEmpty()) {
      throw SubjectAccessRequestNoReports()
    }

    return HmppsSubjectAccessRequestContent(
      content = content,
    )
  }
}
