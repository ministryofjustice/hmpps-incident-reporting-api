package uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.ValidationException
import uk.gov.justice.digital.hmpps.incidentreporting.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.incidentreporting.dto.DescriptionAddendum
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatterBuilder

private const val DATETIME_PATTERN = "\\d{2}-[A-Z]{3}-\\d{4} \\d{2}:\\d{2}"
private val DATETIME_PATTERN_REGEX = DATETIME_PATTERN.toRegex()
private val USER_REGEX = "User:".toRegex()
private val DATE_REGEX = " Date:$DATETIME_PATTERN".toRegex()
private val DATE_FORMATTER = DateTimeFormatterBuilder()
  .parseCaseInsensitive()
  .appendPattern("dd-MMM-yyyy HH:mm")
  .toFormatter()

@Schema(description = "NOMIS Incident Report Details")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class NomisReport(
  @Schema(description = "The Incident id")
  val incidentId: Long,
  @Schema(description = "The id of the questionnaire associated with this incident")
  val questionnaireId: Long,
  @Schema(description = "A summary of the incident")
  val title: String?,
  @Schema(description = "The incident details")
  val description: String?,
  @Schema(description = "Prison where the incident occurred")
  val prison: NomisCode,

  @Schema(description = "Status details")
  val status: NomisStatus,
  @Schema(description = "The incident questionnaire type")
  val type: String,

  @Schema(description = "If the response is locked ie if the response is completed")
  val lockedResponse: Boolean,

  @Schema(description = "The date and time of the incident")
  val incidentDateTime: LocalDateTime,

  @Schema(description = "The staff member who reported the incident")
  val reportingStaff: NomisStaff,
  @Schema(description = "The date and time the incident was reported")
  val reportedDateTime: LocalDateTime,

  @Schema(description = "The date and time the incident was created")
  val createDateTime: LocalDateTime,
  @Schema(description = "The username of the person who created the incident")
  val createdBy: String,

  @Schema(description = "The date and time the incident was last updated")
  val lastModifiedDateTime: LocalDateTime? = createDateTime,
  @Schema(description = "The username of the person who last updated the incident")
  val lastModifiedBy: String? = createdBy,

  @Schema(description = "The follow up date for the incident")
  val followUpDate: LocalDate? = null,

  @Schema(description = "Staff involved in the incident")
  val staffParties: List<NomisStaffParty>,

  @Schema(description = "Offenders involved in the incident")
  val offenderParties: List<NomisOffenderParty>,

  @Schema(description = "Requirements for completing the incident report")
  val requirements: List<NomisRequirement>,

  @Schema(description = "Questions asked for the incident")
  val questions: List<NomisQuestion>,

  @Schema(description = "Historical questionnaire details for the incident")
  val history: List<NomisHistory>,

) {
  fun getDescriptionParts(): Pair<String?, List<DescriptionAddendum>> {
    if (description == null) return Pair(null, emptyList())

    val entries = description.split(USER_REGEX)
    val baseDescription = entries[0]

    if (entries.size == 1) return Pair(baseDescription, emptyList())

    val additionalEntries = entries.drop(1)

    return Pair(
      baseDescription,
      additionalEntries.mapIndexed { index, entry ->

        val (fullName, addText) = entry.split(DATE_REGEX, limit = 2).takeIf { it.size == 2 }
          ?: throw ValidationException("Validation issue: $entry")

        val (lastName, firstName) = fullName.split(",", limit = 2).takeIf { it.size == 2 }
          ?: throw ValidationException("Validation issue: $entry")

        val dateTimeString = DATETIME_PATTERN_REGEX.find(entry)?.value
          ?: throw ValidationException("Validation issue: $entry")

        val createdAt = LocalDateTime.parse(dateTimeString, DATE_FORMATTER)

        DescriptionAddendum(
          sequence = index,
          createdBy = SYSTEM_USERNAME,
          firstName = firstName,
          lastName = lastName,
          createdAt = createdAt,
          text = addText,
        )
      },
    )
  }
}
