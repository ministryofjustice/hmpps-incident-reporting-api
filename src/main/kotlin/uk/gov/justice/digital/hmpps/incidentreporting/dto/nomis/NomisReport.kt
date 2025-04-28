package uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.incidentreporting.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.incidentreporting.dto.DescriptionAddendum
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder

private val TIME_PATTERN_REGEX = Regex("\\d{2}-[A-Z]{3}-\\d{4} \\d{2}:\\d{2}")
private val DATE_REGEX = " Date:\\d{2}-[A-Z]{3}-\\d{4} \\d{2}:\\d{2}".toRegex()

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
    val addendums = mutableListOf<DescriptionAddendum>()

    description?.let {
      val entries = it.split("User:".toRegex())
      val baseDescription = entries[0]

      if (entries.size > 1) {
        val additionalEntries = entries.drop(1)
        val dateTimeFormatter = dateTimeFormatter()
        for (entry in additionalEntries) {
          val fullName = entry.split(" Date:".toRegex())[0]
          val firstName = fullName.split(",")[1]
          val lastName = fullName.split(",")[0]

          val addText = entry.split(DATE_REGEX)[1]
          val dateTimeString = TIME_PATTERN_REGEX.find(entry)?.value ?: RuntimeException("Date not found")

          val createdAt = LocalDateTime.parse(dateTimeString.toString(), dateTimeFormatter)

          addendums.add(
            DescriptionAddendum(
              createdBy = SYSTEM_USERNAME,
              firstName = firstName,
              lastName = lastName,
              createdAt = createdAt,
              text = addText,
            ),
          )
        }
        return Pair(baseDescription, addendums)
      } else {
        return Pair(baseDescription, emptyList())
      }
    }
    return Pair(null, emptyList())
  }

  private fun dateTimeFormatter(): DateTimeFormatter {
    val builder = DateTimeFormatterBuilder()
    builder.parseCaseInsensitive()
    builder.appendPattern("dd-MMM-yyyy HH:mm")
    val dateTimeFormat = builder.toFormatter()
    return dateTimeFormat
  }
}
