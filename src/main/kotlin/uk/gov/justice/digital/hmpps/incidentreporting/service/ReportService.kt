package uk.gov.justice.digital.hmpps.incidentreporting.service

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.incidentreporting.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.incidentreporting.config.trackEvent
import uk.gov.justice.digital.hmpps.incidentreporting.constants.InformationSource
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Status
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Type
import uk.gov.justice.digital.hmpps.incidentreporting.dto.Question
import uk.gov.justice.digital.hmpps.incidentreporting.dto.ReportBasic
import uk.gov.justice.digital.hmpps.incidentreporting.dto.ReportWithDetails
import uk.gov.justice.digital.hmpps.incidentreporting.dto.request.AddDescriptionAddendumRequest
import uk.gov.justice.digital.hmpps.incidentreporting.dto.request.AddOrUpdateQuestionWithResponses
import uk.gov.justice.digital.hmpps.incidentreporting.dto.request.ChangeStatusRequest
import uk.gov.justice.digital.hmpps.incidentreporting.dto.request.ChangeTypeRequest
import uk.gov.justice.digital.hmpps.incidentreporting.dto.request.CreateReportRequest
import uk.gov.justice.digital.hmpps.incidentreporting.dto.request.UpdateReportRequest
import uk.gov.justice.digital.hmpps.incidentreporting.dto.utils.MaybeChanged
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.EventRepository
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.PrisonerInvolvementRepository
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.ReportRepository
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.generateEventReference
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.generateReportReference
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.specifications.filterByIncidentDateFrom
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.specifications.filterByIncidentDateUntil
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.specifications.filterByInvolvedPrisoner
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.specifications.filterByInvolvedStaff
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.specifications.filterByLocations
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.specifications.filterByReference
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.specifications.filterByReportedBy
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.specifications.filterByReportedDateFrom
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.specifications.filterByReportedDateUntil
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.specifications.filterBySource
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.specifications.filterByStatuses
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.specifications.filterByTypes
import uk.gov.justice.digital.hmpps.incidentreporting.resource.EventNotFoundException
import uk.gov.justice.hmpps.kotlin.auth.HmppsAuthenticationHolder
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.jvm.optionals.getOrNull
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.Report as ReportEntity

@Service
@Transactional(readOnly = true)
class ReportService(
  private val reportRepository: ReportRepository,
  private val eventRepository: EventRepository,
  private val prisonerInvolvementRepository: PrisonerInvolvementRepository,
  private val prisonerSearchService: PrisonerSearchService,
  private val telemetryClient: TelemetryClient,
  private val authenticationHolder: HmppsAuthenticationHolder,
  private val clock: Clock,
) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getBasicReports(
    reference: String? = null,
    locations: List<String> = emptyList(),
    source: InformationSource? = null,
    statuses: List<Status> = emptyList(),
    types: List<Type> = emptyList(),
    incidentDateFrom: LocalDate? = null,
    incidentDateUntil: LocalDate? = null,
    reportedDateFrom: LocalDate? = null,
    reportedDateUntil: LocalDate? = null,
    reportedByUsername: String? = null,
    involvingStaffUsername: String? = null,
    involvingPrisonerNumber: String? = null,
    pageable: Pageable = PageRequest.of(0, 20, Sort.by("incidentDateAndTime").descending()),
  ): Page<ReportBasic> {
    val specification = Specification.allOf(
      buildList {
        reference?.let { add(filterByReference(reference)) }
        if (locations.isNotEmpty()) {
          add(filterByLocations(locations))
        }
        source?.let { add(filterBySource(source)) }
        if (statuses.isNotEmpty()) {
          add(filterByStatuses(statuses))
        }
        if (types.isNotEmpty()) {
          add(filterByTypes(types))
        }
        incidentDateFrom?.let { add(filterByIncidentDateFrom(incidentDateFrom)) }
        incidentDateUntil?.let { add(filterByIncidentDateUntil(incidentDateUntil)) }
        reportedDateFrom?.let { add(filterByReportedDateFrom(reportedDateFrom)) }
        reportedDateUntil?.let { add(filterByReportedDateUntil(reportedDateUntil)) }
        reportedByUsername?.let { add(filterByReportedBy(reportedByUsername)) }
        involvingStaffUsername?.let { add(filterByInvolvedStaff(involvingStaffUsername)) }
        involvingPrisonerNumber?.let { add(filterByInvolvedPrisoner(involvingPrisonerNumber)) }
      },
    )
    return reportRepository.findAll(specification, pageable)
      .map { it.toDtoBasic() }
  }

  fun getBasicReportById(id: UUID): ReportBasic? {
    return reportRepository.findById(id).getOrNull()
      ?.toDtoBasic()
  }

  fun getBasicReportByReference(reportReference: String): ReportBasic? {
    return reportRepository.findOneByReportReference(reportReference)
      ?.toDtoBasic()
  }

  fun getReportWithDetailsById(id: UUID): ReportWithDetails? {
    return reportRepository.findOneEagerlyById(id)
      ?.toDtoWithDetails()
  }

  fun getReportWithDetailsByReference(reportReference: String): ReportWithDetails? {
    return reportRepository.findOneEagerlyByReportReference(reportReference)
      ?.toDtoWithDetails()
  }

  @Transactional
  fun deleteReportById(id: UUID, deleteOrphanedEvents: Boolean = true): ReportWithDetails? {
    return reportRepository.findOneEagerlyById(id)?.let { report ->
      val eventIdToDelete = if (deleteOrphanedEvents && report.event.reports.size == 1) {
        report.event.id!!
      } else {
        null
      }
      report.toDtoWithDetails().also {
        report.event.reports.removeIf { it.id == id }
        reportRepository.deleteById(id)

        log.info("Deleted incident report reference=${report.reportReference} ID=${report.id}")
        telemetryClient.trackEvent(
          "Deleted incident report",
          it,
        )

        eventIdToDelete?.let { eventId ->
          eventRepository.deleteById(eventId)

          log.info("Deleted event ID=$eventId")
        }
      }
    }
  }

  @Transactional
  fun createReport(createReportRequest: CreateReportRequest): ReportWithDetails {
    val now = LocalDateTime.now(clock)
    val requestUsername = authenticationHolder.username ?: SYSTEM_USERNAME

    createReportRequest.validate(now = now)

    val event = if (createReportRequest.linkedEventReference != null) {
      eventRepository.findOneByEventReference(createReportRequest.linkedEventReference)
        ?: throw EventNotFoundException(createReportRequest.linkedEventReference)
    } else {
      createReportRequest.createEvent(
        eventRepository.generateEventReference(),
        requestUsername = requestUsername,
        now = now,
      )
    }

    val unsavedReport = createReportRequest.createReport(
      reportReference = reportRepository.generateReportReference(),
      event = event,
      requestUsername = requestUsername,
      now = now,
    )

    val report = reportRepository.save(unsavedReport).toDtoWithDetails()

    log.info("Created draft incident report reference=${report.reportReference} ID=${report.id}")
    telemetryClient.trackEvent(
      "Created draft incident report",
      report,
    )

    return report
  }

  @Transactional
  fun updateReport(id: UUID, updateReportRequest: UpdateReportRequest): ReportBasic? {
    val now = LocalDateTime.now(clock)
    val requestUsername = authenticationHolder.username ?: SYSTEM_USERNAME

    updateReportRequest.validate(now)

    return reportRepository.findById(id).map {
      updateReportRequest.updateExistingReport(
        report = it,
        requestUsername = requestUsername,
        now = now,
      ).toDtoBasic().apply {
        val changeMessage = if (updateReportRequest.updateEvent) {
          "Updated incident report and event"
        } else {
          "Updated incident report"
        }
        log.info("$changeMessage reference=$reportReference ID=$id")
        telemetryClient.trackEvent(
          changeMessage,
          this,
        )
      }
    }.getOrNull()
  }

  @Transactional
  fun addDescriptionAddendum(id: UUID, request: AddDescriptionAddendumRequest): ReportWithDetails? {
    val now = LocalDateTime.now(clock)
    val user = authenticationHolder.username ?: SYSTEM_USERNAME

    return reportRepository.findOneEagerlyById(id)?.let { reportEntity ->
      reportEntity.addDescriptionAddendum(
        createdBy = request.createdBy,
        firstName = request.firstName,
        lastName = request.lastName,
        createdAt = request.createdAt ?: now,
        text = request.text,
      )

      reportEntity.modifiedIn = InformationSource.DPS
      reportEntity.modifiedAt = now
      reportEntity.modifiedBy = user

      val reportWithDetails = reportEntity.toDtoWithDetails()
      log.info(
        "Appended to incident report description for reference=${reportWithDetails.reportReference} ID=$id",
      )
      telemetryClient.trackEvent(
        "Appended to incident report description",
        reportWithDetails,
      )

      reportWithDetails
    }
  }

  @Transactional
  fun changeReportStatus(id: UUID, changeStatusRequest: ChangeStatusRequest): MaybeChanged<ReportWithDetails>? {
    return reportRepository.findById(id).getOrNull()?.let { reportEntity ->
      val maybeChangedReport = if (reportEntity.status != changeStatusRequest.newStatus) {
        // TODO: determine which transitions are allowed

        val now = LocalDateTime.now(clock)
        val user = authenticationHolder.username ?: SYSTEM_USERNAME
        reportEntity.changeStatus(
          newStatus = changeStatusRequest.newStatus,
          changedAt = now,
          changedBy = user,
        )
        reportEntity.modifiedIn = InformationSource.DPS
        reportEntity.modifiedAt = now
        reportEntity.modifiedBy = user

        MaybeChanged.Changed(reportEntity.toDtoWithDetails())
      } else {
        MaybeChanged.Unchanged(reportEntity.toDtoWithDetails())
      }

      maybeChangedReport.alsoIfChanged { reportWithDetails ->
        log.info(
          "Changed incident report status to ${changeStatusRequest.newStatus} for reference=${reportWithDetails.reportReference} ID=$id",
        )
        telemetryClient.trackEvent(
          "Changed incident report status",
          reportWithDetails,
        )
      }
    }
  }

  @Transactional
  fun changeReportType(id: UUID, changeTypeRequest: ChangeTypeRequest): MaybeChanged<ReportWithDetails>? {
    changeTypeRequest.validate()

    return reportRepository.findOneEagerlyById(id)?.let { reportEntity ->
      val maybeChangedReport = if (reportEntity.type != changeTypeRequest.newType) {
        val now = LocalDateTime.now(clock)
        val user = authenticationHolder.username ?: SYSTEM_USERNAME
        reportEntity.changeType(
          newType = changeTypeRequest.newType,
          changedAt = now,
          changedBy = user,
        )
        reportEntity.modifiedIn = InformationSource.DPS
        reportEntity.modifiedAt = now
        reportEntity.modifiedBy = user

        MaybeChanged.Changed(reportEntity.toDtoWithDetails())
      } else {
        MaybeChanged.Unchanged(reportEntity.toDtoWithDetails())
      }

      maybeChangedReport.alsoIfChanged { reportWithDetails ->
        log.info(
          "Changed incident report type to ${changeTypeRequest.newType} for reference=${reportWithDetails.reportReference} ID=$id",
        )
        telemetryClient.trackEvent(
          "Changed incident report type",
          reportWithDetails,
        )
      }
    }
  }

  fun getQuestionsWithResponses(reportId: UUID): List<Question>? {
    return reportRepository.findOneEagerlyById(reportId)?.run {
      questions.map { it.toDto() }
    }
  }

  @Transactional
  fun addOrUpdateQuestionsWithResponses(
    reportId: UUID,
    requests: List<AddOrUpdateQuestionWithResponses>,
  ): Pair<ReportBasic, List<Question>>? {
    return reportRepository.findById(reportId).getOrNull()?.run {
      val now = LocalDateTime.now(clock)
      val requestUsername = authenticationHolder.username ?: SYSTEM_USERNAME

      val questionMap = questions.associateBy { question -> question.code }
      var addedCount = 0
      var updatedCount = 0

      requests.forEach { request ->
        val existingQuestion = questionMap[request.code]
        with(
          if (existingQuestion != null) {
            updatedCount += 1
            existingQuestion.reset(
              question = request.question,
              additionalInformation = request.additionalInformation,
            )
          } else {
            addedCount += 1
            val sequence = if (this.questions.isEmpty()) 1 else this.questions.last().sequence + 1
            addQuestion(
              code = request.code,
              question = request.question,
              sequence = sequence,
              additionalInformation = request.additionalInformation,
            )
          },
        ) {
          var sequence = if (this.responses.isEmpty()) 0 else this.responses.last().sequence + 1
          request.responses.forEach {
            addResponse(
              response = it.response,
              sequence = sequence,
              responseDate = it.responseDate,
              recordedBy = requestUsername,
              recordedAt = now,
              additionalInformation = it.additionalInformation,
            )
            sequence += 1
          }
        }
      }

      modifiedIn = InformationSource.DPS
      modifiedAt = now
      modifiedBy = requestUsername

      val reportBasic = toDtoBasic()

      log.info(
        "Added $addedCount and updated $updatedCount question(s) with responses in report reference=$reportReference ID=$id",
      )
      telemetryClient.trackEvent(
        "Added $addedCount and updated $updatedCount question(s) with responses",
        reportBasic,
      )

      reportBasic to questions.map { it.toDto() }
    }
  }

  @Transactional
  fun deleteQuestionsAndResponses(reportId: UUID, questionCodes: Set<String>): Pair<ReportBasic, List<Question>>? {
    return reportRepository.findOneEagerlyById(reportId)?.run {
      removeQuestions(questionCodes)

      modifiedIn = InformationSource.DPS
      modifiedAt = LocalDateTime.now(clock)
      modifiedBy = authenticationHolder.username ?: SYSTEM_USERNAME

      val reportBasic = toDtoBasic()

      log.info("Deleted last question and responses from report reference=$reportReference ID=$id")
      telemetryClient.trackEvent(
        "Deleted last question and responses",
        reportBasic,
      )

      reportBasic to questions.map { it.toDto() }
    }
  }

  /**
   * Replaces one prisoner number in all report-related entities with another.
   *
   * Relevant reports are found from prisoner involvements.
   *
   * This is used to handle a two prisoner numbers being merged into one.
   *
   * NB:
   * - free text fields are _not_ changed
   * - report-amended domain event is _not_ raised since this method is reacting to an event
   */
  @Transactional
  fun replacePrisonerNumber(removedPrisonerNumber: String, prisonerNumber: String): List<ReportBasic> {
    return replacePrisonerNumberInDateRange(removedPrisonerNumber, prisonerNumber, null, null)
  }

  /**
   * Replaces one prisoner number in all report-related entities with another within given inclusive date-time range
   * according to when an incident was reported (`Report.reportedAt`). The date-time range can be open or absent.
   *
   * Relevant reports are found from prisoner involvements.
   *
   * This is used to handle a single booking being moved between two prisoner numbers.
   *
   * NB:
   * - free text fields are _not_ changed
   * - report-amended domain event is _not_ raised since this method is reacting to an event
   */
  @Transactional
  fun replacePrisonerNumberInDateRange(
    removedPrisonerNumber: String,
    prisonerNumber: String,
    since: LocalDateTime?,
    until: LocalDateTime?,
  ): List<ReportBasic> {
    // TODO: maybe free text fields should be replaced too? especially if we end up generating report titles automatically

    val prisoner by lazy {
      prisonerSearchService.searchByPrisonerNumbers(setOf(prisonerNumber))[prisonerNumber]!!
    }

    val reportedAtFilter: (ReportEntity) -> Boolean = when {
      // reported between `since` and `until`
      since != null && until != null -> { report -> report.reportedAt >= since && report.reportedAt <= until }
      // reported after `since`
      since != null -> { report -> report.reportedAt >= since }
      // reported before `until`
      until != null -> { report -> report.reportedAt <= until }
      // reported at any time
      else -> { _ -> true }
    }

    val now = LocalDateTime.now(clock)
    return buildMap {
      prisonerInvolvementRepository.findAllByPrisonerNumber(removedPrisonerNumber)
        .forEach { prisonerInvolvement ->
          val report = prisonerInvolvement.report
          if (!containsKey(report.id)) {
            put(report.id, report)
          }
        }
    }
      .values
      .filter(reportedAtFilter)
      .map { report ->
        // mark report as kinda-modified in case downstream consumers use this to spot changes
        report.modifiedAt = now
        // NB: report.modifiedIn is not changed since currently only a NOMIS-sourced domain event triggers this
        // NB: there is no actor user to set report.modifiedBy

        // replace prisoner number in matching prisoner involvements
        report.prisonersInvolved.filter { prisonerInvolvement ->
          prisonerInvolvement.prisonerNumber == removedPrisonerNumber
        }.forEach { prisonerInvolvement ->
          prisonerInvolvement.prisonerNumber = prisonerNumber
          prisonerInvolvement.firstName = prisoner.firstName
          prisonerInvolvement.lastName = prisoner.lastName
        }

        report.toDtoBasic()
      }
      .sortedBy { it.id }
      .also { reports ->
        if (reports.isNotEmpty()) {
          val logMessage = "Prisoner $removedPrisonerNumber replaced with $prisonerNumber " +
            "(reported between ${since ?: "whenever"} and ${until ?: "now"})"
          log.info(logMessage)
          reports.forEach { report ->
            telemetryClient.trackEvent(logMessage, report)
          }
        }
      }
  }
}
