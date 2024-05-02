package uk.gov.justice.digital.hmpps.incidentreporting.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.incidentreporting.constants.PrisonerOutcome
import uk.gov.justice.digital.hmpps.incidentreporting.constants.PrisonerRole
import uk.gov.justice.digital.hmpps.incidentreporting.constants.StaffRole
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Status
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Type
import uk.gov.justice.digital.hmpps.incidentreporting.dto.response.ConstantDescription
import uk.gov.justice.digital.hmpps.incidentreporting.dto.response.DeactivatableConstantDescription

@RestController
@Validated
@RequestMapping("/constants", produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(name = "Constants", description = "Constants and enumerations used in incident reports")
class ConstantsResource {
  @GetMapping("/prisoner-outcomes")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "List codes and descriptions of outcomes from a prisoner’s involvement in an incident",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Returns codes and descriptions",
      ),
    ],
  )
  fun prisonerOutcomes(): List<ConstantDescription> {
    return PrisonerOutcome.entries.map {
      ConstantDescription(it.name, it.description)
    }
  }

  @GetMapping("/prisoner-roles")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "List codes and descriptions of roles of a prisoner’s involvement in an incident",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Returns codes and descriptions",
      ),
    ],
  )
  fun prisonerRoles(): List<ConstantDescription> {
    return PrisonerRole.entries.map {
      ConstantDescription(it.name, it.description)
    }
  }

  @GetMapping("/staff-roles")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "List codes and descriptions of roles of staff involvement in an incident",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Returns codes and descriptions",
      ),
    ],
  )
  fun staffRoles(): List<ConstantDescription> {
    return StaffRole.entries.map {
      ConstantDescription(it.name, it.description)
    }
  }

  @GetMapping("/statuses")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "List codes and descriptions of incident statuses",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Returns codes and descriptions",
      ),
    ],
  )
  fun statuses(): List<ConstantDescription> {
    return Status.entries.map {
      ConstantDescription(it.name, it.description)
    }
  }

  @GetMapping("/types")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "List codes and descriptions of incident types",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Returns codes and descriptions",
      ),
    ],
  )
  fun types(): List<DeactivatableConstantDescription> {
    return Type.entries.map {
      DeactivatableConstantDescription(it.name, it.description, it.active)
    }
  }
}
