package uk.gov.justice.digital.hmpps.incidentreporting.dto.response

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Report incident type constant", accessMode = Schema.AccessMode.READ_ONLY)
@JsonInclude(JsonInclude.Include.ALWAYS)
data class TypeConstantDescription(
  @param:Schema(description = "Machine-readable identifier for this family of incident types", example = "DISORDER")
  val familyCode: String,
  @param:Schema(description = "Machine-readable identifier of this incident type", example = "DISORDER")
  val code: String,
  @param:Schema(
    description = "Human-readable description of this family of incident types " +
      "(all incident types in one family share a description)",
    example = "Disorder",
  )
  val description: String,
  @param:Schema(description = "Whether this type is currently active and usable in new reports", example = "true")
  val active: Boolean = true,

  // NB: this property can be removed once fully migrated off NOMIS and reconciliation checks are turned off
  @param:Schema(
    description = "Machine-readable NOMIS identifier of this incident type, which may be null for newer ones",
    nullable = true,
    example = "DISORDER1",
    deprecated = true,
  )
  val nomisCode: String?,
)
