package uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.ValidationException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import uk.gov.justice.digital.hmpps.incidentreporting.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.incidentreporting.dto.DescriptionAddendum
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatterBuilder
import java.time.format.DateTimeParseException

private const val DATETIME_PATTERN = "\\d{1,2}[-/](?:[A-Z]{3}|\\d{2})[-/]\\d{4} \\d{2}:\\d{2}"

private val DATETIME_PATTERN_REGEX = DATETIME_PATTERN.toRegex()
private val DATE_REGEX = " Date:$DATETIME_PATTERN".toRegex()

private val DATE_FORMATTER_1 = DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern(
  "dd-MMM-yyyy HH:mm",
).toFormatter()
private val DATE_FORMATTER_2 = DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern(
  "dd/MM/yyyy HH:mm",
).toFormatter()

@Schema(description = "NOMIS Incident Report Details")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class NomisReport(
  @param:Schema(description = "The Incident id")
  val incidentId: Long,
  @param:Schema(description = "The id of the questionnaire associated with this incident")
  val questionnaireId: Long,
  @param:Schema(description = "A summary of the incident")
  val title: String?,
  @param:Schema(description = "The incident details")
  val description: String?,
  @param:Schema(description = "Prison where the incident occurred")
  val prison: NomisCode,

  @param:Schema(description = "Status details")
  val status: NomisStatus,
  @param:Schema(description = "The incident questionnaire type")
  val type: String,

  @param:Schema(description = "If the response is locked ie if the response is completed")
  val lockedResponse: Boolean,

  @param:Schema(description = "The date and time of the incident")
  val incidentDateTime: LocalDateTime,

  @param:Schema(description = "The staff member who reported the incident")
  val reportingStaff: NomisStaff,
  @param:Schema(description = "The date and time the incident was reported")
  val reportedDateTime: LocalDateTime,

  @param:Schema(description = "The date and time the incident was created")
  val createDateTime: LocalDateTime,
  @param:Schema(description = "The username of the person who created the incident")
  val createdBy: String,

  @param:Schema(description = "The date and time the incident was last updated")
  val lastModifiedDateTime: LocalDateTime? = createDateTime,
  @param:Schema(description = "The username of the person who last updated the incident")
  val lastModifiedBy: String? = createdBy,

  @param:Schema(description = "The follow up date for the incident")
  val followUpDate: LocalDate? = null,

  @param:Schema(description = "Staff involved in the incident")
  val staffParties: List<NomisStaffParty>,

  @param:Schema(description = "Offenders involved in the incident")
  val offenderParties: List<NomisOffenderParty>,

  @param:Schema(description = "Requirements for completing the incident report")
  val requirements: List<NomisRequirement>,

  @param:Schema(description = "Questions asked for the incident")
  val questions: List<NomisQuestion>,

  @param:Schema(description = "Historical questionnaire details for the incident")
  val history: List<NomisHistory>,
) {

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getDescriptionParts(): Pair<String?, List<DescriptionAddendum>> {
    try {
      if (description == null) return Pair(null, emptyList())

      val entries = description.split("User:")
      val baseDescription = entries[0]

      if (entries.size == 1) return Pair(baseDescription, emptyList())
      val additionalEntries = entries.drop(1)

      return Pair(baseDescription, additionalEntries.mapIndexed { index, entry -> buildAddendum(entry, index) })
    } catch (e: ValidationException) {
      log.error("Validation issue with incident details, skipping split out of appended information: $description", e)
      return Pair(description, emptyList())
    }
  }

  private fun buildAddendum(entry: String, index: Int): DescriptionAddendum {
    val (fullName, addText) = entry.split(DATE_REGEX, limit = 2).takeIf { it.size == 2 }
      ?: throw ValidationException("Validation issue, date pattern not found: $entry")

    val (lastName, firstName) = fullName.split(",", limit = 2).takeIf { it.size == 2 }
      ?: throw ValidationException("Validation issue, first and last name not found in format expected: $entry")

    val dateTimeString = DATETIME_PATTERN_REGEX.find(entry)?.value
      ?: throw ValidationException("Validation issue, date is not in format expected: $entry")

    val createdAt = try {
      LocalDateTime.parse(dateTimeString, DATE_FORMATTER_1)
    } catch (@Suppress("unused") e: DateTimeParseException) {
      LocalDateTime.parse(dateTimeString, DATE_FORMATTER_2)
    }

    return DescriptionAddendum(
      sequence = index,
      createdBy = SYSTEM_USERNAME,
      createdAt = createdAt,
      firstName = firstName,
      lastName = lastName,
      text = addText,
    )
  }
}
