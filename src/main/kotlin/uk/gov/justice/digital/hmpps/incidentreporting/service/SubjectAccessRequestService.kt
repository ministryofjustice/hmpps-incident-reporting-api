package uk.gov.justice.digital.hmpps.incidentreporting.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.incidentreporting.dto.sar.SarIncidentReport
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.SubjectAccessRequestRepository
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.fetchDetails
import uk.gov.justice.hmpps.kotlin.sar.HmppsPrisonSubjectAccessRequestService
import uk.gov.justice.hmpps.kotlin.sar.HmppsSubjectAccessRequestContent
import java.time.LocalDate

/**
 * Serves every incident report a prisoner is named on for a subject access request.
 *
 * Declaring this bean is the whole wiring: hmpps-kotlin-spring-boot-starter registers
 * `GET /subject-access-request` because a [HmppsPrisonSubjectAccessRequestService] is present, and guards it with
 * the SAR_DATA_ACCESS role. The report template is served from `GET /subject-access-request/template`, configured
 * under `hmpps.sar.template`.
 *
 * A report is found when the prisoner is recorded on it as someone involved. A report that only mentions them in
 * its written description is not found - there is nothing to search on but free text. Reports in every status are
 * returned, including drafts, duplicates and reports judged not reportable. Both points are put to the Offender SAR
 * team at the data review (IR-1461).
 */
@Service
class SubjectAccessRequestService(
  private val subjectAccessRequestRepository: SubjectAccessRequestRepository,
) : HmppsPrisonSubjectAccessRequestService {

  /**
   * [fromDate] and [toDate] are both inclusive and apply to the date of the incident. [toDate] is widened to the
   * start of the following day so that an incident on that date is caught whatever the time.
   *
   * The incident date rather than when the report was last changed, because a request for a period asks what
   * happened in it, and because merging two prison numbers rewrites the last-changed time of every report the old
   * number was on. Which date to use is one of the questions for the Offender SAR team at the data review.
   *
   * Returns null when the prisoner is on no reports, which the library controller turns into a 204.
   */
  @Transactional(readOnly = true)
  override fun getPrisonContentFor(
    prn: String,
    fromDate: LocalDate?,
    toDate: LocalDate?,
  ): HmppsSubjectAccessRequestContent? {
    val reports = subjectAccessRequestRepository.findReports(
      prisonerNumber = prn,
      from = fromDate?.atStartOfDay(),
      to = toDate?.plusDays(1)?.atStartOfDay(),
    )
    if (reports.isEmpty()) {
      return null
    }

    subjectAccessRequestRepository.fetchDetails(reports)
    return HmppsSubjectAccessRequestContent(
      content = reports.map { SarIncidentReport.from(it, prn) },
    )
  }
}
