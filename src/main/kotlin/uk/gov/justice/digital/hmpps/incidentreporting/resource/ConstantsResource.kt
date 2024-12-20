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
import uk.gov.justice.digital.hmpps.incidentreporting.constants.InformationSource
import uk.gov.justice.digital.hmpps.incidentreporting.constants.PrisonerOutcome
import uk.gov.justice.digital.hmpps.incidentreporting.constants.PrisonerRole
import uk.gov.justice.digital.hmpps.incidentreporting.constants.StaffRole
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Status
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Type
import uk.gov.justice.digital.hmpps.incidentreporting.dto.response.ConstantDescription
import uk.gov.justice.digital.hmpps.incidentreporting.dto.response.PrisonerOutcomeConstantDescription
import uk.gov.justice.digital.hmpps.incidentreporting.dto.response.PrisonerRoleConstantDescription
import uk.gov.justice.digital.hmpps.incidentreporting.dto.response.StaffRoleConstantDescription
import uk.gov.justice.digital.hmpps.incidentreporting.dto.response.StatusConstantDescription
import uk.gov.justice.digital.hmpps.incidentreporting.dto.response.TypeConstantDescription

@RestController
@Validated
@RequestMapping("/constants", produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(name = "Constants", description = "Constants and enumerations used in incident reports")
class ConstantsResource {
  @GetMapping("/error-codes")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "List codes used to discriminate between error types",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Returns codes",
      ),
    ],
  )
  fun errorCodes(): List<ConstantDescription> {
    return ErrorCode.entries.map {
      ConstantDescription(it.errorCode.toString(), it.name)
    }
  }

  @GetMapping("/information-sources")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "List codes and descriptions of information sources for incident reports",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Returns codes and descriptions",
      ),
    ],
  )
  fun informationSource(): List<ConstantDescription> {
    return InformationSource.entries.map {
      ConstantDescription(it.name, it.name)
    }
  }

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
  fun prisonerOutcomes(): List<PrisonerOutcomeConstantDescription> {
    return PrisonerOutcome.entries.map {
      PrisonerOutcomeConstantDescription(it.name, it.description, it.nomisCode)
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
  fun prisonerRoles(): List<PrisonerRoleConstantDescription> {
    return PrisonerRole.entries.map {
      PrisonerRoleConstantDescription(it.name, it.description, it.nomisCode)
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
  fun staffRoles(): List<StaffRoleConstantDescription> {
    return StaffRole.entries.map {
      StaffRoleConstantDescription(it.name, it.description, it.nomisCodes.asList())
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
  fun statuses(): List<StatusConstantDescription> {
    return Status.entries.map {
      StatusConstantDescription(it.name, it.description, it.nomisStatus)
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
  fun types(): List<TypeConstantDescription> {
    return Type.entries.map {
      TypeConstantDescription(it.name, it.description, it.active, it.nomisType)
    }
  }
}
