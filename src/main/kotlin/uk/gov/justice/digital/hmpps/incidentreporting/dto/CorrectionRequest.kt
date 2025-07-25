package uk.gov.justice.digital.hmpps.incidentreporting.dto

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.incidentreporting.constants.UserAction
import uk.gov.justice.digital.hmpps.incidentreporting.constants.UserType
import java.time.LocalDateTime

@Schema(description = "Request to make a correction to incident report", accessMode = Schema.AccessMode.READ_ONLY)
@JsonInclude(JsonInclude.Include.ALWAYS)
data class CorrectionRequest(
  // TODO: sequences are only being exposed while we sort out sync problems: they do not need to remain in the api contract
  @param:Schema(description = "Sequence of the correction requests for this report", deprecated = true)
  val sequence: Int,
  @param:Schema(description = "The changes being requested")
  val descriptionOfChange: String,
  @param:Schema(description = "Member of staff requesting changed")
  val correctionRequestedBy: String,
  @param:Schema(description = "When the changes were requested", example = "2024-04-29T12:34:56.789012")
  val correctionRequestedAt: LocalDateTime,
  @param:Schema(description = "The reporting location of the staff")
  val location: String? = null,
  @param:Schema(description = "Action taken by the user on the report")
  val userAction: UserAction? = null,
  @param:Schema(description = "Reference number of the original report of which this report is a duplicate of")
  val originalReportReference: String? = null,
  @param:Schema(description = "Type of user that submitted this action on the report")
  val userType: UserType? = null,
)
