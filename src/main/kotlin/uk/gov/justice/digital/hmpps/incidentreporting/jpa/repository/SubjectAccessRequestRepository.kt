package uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.Repository
import org.springframework.data.repository.query.Param
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.HistoricalQuestion
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.Report
import java.time.LocalDateTime
import java.util.UUID

/**
 * Reads for the subject access request response, kept apart from [ReportRepository] because they exist only to
 * load whole reports for one prisoner efficiently.
 *
 * A report has eight child collections, two of them nested. Loading them lazily costs several queries per report,
 * and loading them in one query with an entity graph multiplies the rows by every collection at once. So
 * [findReports] selects the reports, and each `fetch*` query then fills one collection for all of them in a single
 * query. They run in the same transaction, so each query initialises the collections on the reports already loaded;
 * their return values are not needed. Nine queries however many reports a prisoner is named on.
 */
interface SubjectAccessRequestRepository : Repository<Report, UUID> {
  /**
   * Reports the prisoner is named on, newest incident first. [from] is inclusive and [to] exclusive; either may be
   * null for an open-ended range. The range applies to when the incident happened.
   */
  @Query(
    """
    select r from Report r
    where exists (
      select 1 from PrisonerInvolvement p
      where p.report = r and p.prisonerNumber = :prisonerNumber
    )
    and (cast(:from as LocalDateTime) is null or r.incidentDateAndTime >= :from)
    and (cast(:to as LocalDateTime) is null or r.incidentDateAndTime < :to)
    order by r.incidentDateAndTime desc, r.reportReference desc
    """,
  )
  fun findReports(
    @Param("prisonerNumber") prisonerNumber: String,
    @Param("from") from: LocalDateTime?,
    @Param("to") to: LocalDateTime?,
  ): List<Report>

  @Query("select r from Report r left join fetch r.descriptionAddendums where r.id in :ids")
  fun fetchDescriptionAddendums(@Param("ids") ids: Collection<UUID>): List<Report>

  @Query("select r from Report r left join fetch r.prisonersInvolved where r.id in :ids")
  fun fetchPrisonersInvolved(@Param("ids") ids: Collection<UUID>): List<Report>

  @Query("select r from Report r left join fetch r.staffInvolved where r.id in :ids")
  fun fetchStaffInvolved(@Param("ids") ids: Collection<UUID>): List<Report>

  @Query("select r from Report r left join fetch r.questions q left join fetch q.responses where r.id in :ids")
  fun fetchQuestions(@Param("ids") ids: Collection<UUID>): List<Report>

  @Query("select r from Report r left join fetch r.correctionRequests where r.id in :ids")
  fun fetchCorrectionRequests(@Param("ids") ids: Collection<UUID>): List<Report>

  @Query("select r from Report r left join fetch r.historyOfStatuses where r.id in :ids")
  fun fetchStatusHistory(@Param("ids") ids: Collection<UUID>): List<Report>

  @Query("select r from Report r left join fetch r.history h left join fetch h.questions where r.id in :ids")
  fun fetchTypeHistory(@Param("ids") ids: Collection<UUID>): List<Report>

  @Query("select hq from HistoricalQuestion hq left join fetch hq.responses where hq.history.report.id in :ids")
  fun fetchHistoricalResponses(@Param("ids") ids: Collection<UUID>): List<HistoricalQuestion>
}

/**
 * Loads every collection the subject access request response reads, for all the given reports at once. Must run
 * in the same transaction as the query that loaded [reports].
 */
fun SubjectAccessRequestRepository.fetchDetails(reports: List<Report>) {
  val ids = reports.map { it.id!! }
  fetchDescriptionAddendums(ids)
  fetchPrisonersInvolved(ids)
  fetchStaffInvolved(ids)
  fetchQuestions(ids)
  fetchCorrectionRequests(ids)
  fetchStatusHistory(ids)
  fetchTypeHistory(ids)
  fetchHistoricalResponses(ids)
}
