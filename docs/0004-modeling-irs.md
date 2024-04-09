# 4. A new modelling for the Incident Reporting Service

[Next >>](9999-end.md)


Date: 2024-03-04

## Status

Accepted

## Context
This document details the new design of Incident Reports that will support old NOMIS model and a new more flexible model for the Incident Reporting Service.

```mermaid

classDiagram
    direction BT
    class CorrectionReason {
        <<enumeration>>
        EnumEntries~CorrectionReason~ entries
    }
    class Evidence {
        String descriptionOfEvidence
        Long? id
        String typeOfEvidence
        IncidentReport incident
    }
    class HistoricalIncidentResponse {
        IncidentLocation? location
        PrisonerInvolvement? prisonerInvolvement
        Long? id
        StaffInvolvement? staffInvolvement
        List~HistoricalResponse~ responses
        OtherPersonInvolvement? otherPersonInvolvement
        Evidence? evidence
        IncidentReport incident
        String dataItem
        String? dataItemDescription
    }
    class HistoricalResponse {
        String? additionalInformation
        Long? id
        HistoricalIncidentResponse incidentResponse
        LocalDateTime recordedOn
        String recordedBy
        String itemValue
    }
    class IncidentCorrectionRequest {
        CorrectionReason reason
        String correctionRequestedBy
        String? descriptionOfChange
        Long? id
        IncidentReport incident
        LocalDateTime correctionRequestedAt
    }
    class IncidentEvent {
        String eventDetails
        String prisonId
        String eventId
        Long? id
        String lastModifiedBy
        LocalDateTime createdDate
        LocalDateTime lastModifiedDate
        List~IncidentReport~ incidents
        LocalDateTime eventDateAndTime
    }
    class IncidentLocation {
        String locationType
        String locationId
        Long? id
        IncidentReport incident
        String? locationDescription
    }
    class IncidentReport {
        String prisonId
        List~Evidence~ evidence
        String? summary
        LocalDateTime reportedDate
        List~PrisonerInvolvement~ prisonersInvolved
        String incidentDetails
        IncidentEvent event
        List~IncidentCorrectionRequest~ incidentCorrectionRequests
        LocalDateTime incidentDateAndTime
        List~StaffInvolvement~ staffInvolved
        String? questionSetId
        String incidentNumber
        List~OtherPersonInvolvement~ otherPeopleInvolved
        List~IncidentResponse~ incidentResponses
        List~HistoricalIncidentResponse~ historyOfResponses
        String assignedTo
        UUID? id
        List~IncidentLocation~ locations
        String reportedBy
        String lastModifiedBy
        List~StatusHistory~ historyOfStatuses
        LocalDateTime lastModifiedDate
        LocalDateTime createdDate
        IncidentStatus status
        IncidentType incidentType
        InformationSource source
    }
    class IncidentResponse {
        IncidentLocation? location
        PrisonerInvolvement? prisonerInvolvement
        Long? id
        StaffInvolvement? staffInvolvement
        List~Response~ responses
        OtherPersonInvolvement? otherPersonInvolvement
        Evidence? evidence
        IncidentReport incident
        String dataItem
        String? dataItemDescription
    }
    class IncidentStatus {
        <<enumeration>>
        EnumEntries~IncidentStatus~ entries
    }
    class IncidentType {
        <<enumeration>>
        String description
        EnumEntries~IncidentType~ entries
    }
    class OtherPersonInvolvement {
        PersonRole personType
        Long? id
        String personName
        IncidentReport incident
    }
    class PersonRole {
        <<enumeration>>
        EnumEntries~PersonRole~ entries
    }
    class PrisonerInvolvement {
        PrisonerRole prisonerInvolvement
        String prisonerNumber
        String? comment
        Long? id
        IncidentReport incident
        PrisonerOutcome? outcome
    }
    class PrisonerOutcome {
        <<enumeration>>
        String description
        EnumEntries~PrisonerOutcome~ entries
    }
    class PrisonerRole {
        <<enumeration>>
        String description
        EnumEntries~PrisonerRole~ entries
    }
    class Response {
        String? additionalInformation
        Long? id
        IncidentResponse incidentResponse
        LocalDateTime recordedOn
        String recordedBy
        String itemValue
    }
    class StaffInvolvement {
        String? comment
        StaffRole staffRole
        Long? id
        String staffUsername
        IncidentReport incident
    }
    class StaffRole {
        <<enumeration>>
        String description
        EnumEntries~StaffRole~ entries
    }
    class StatusHistory {
        Long? id
        String setBy
        IncidentStatus status
        LocalDateTime setOn
        IncidentReport incident
    }

    Evidence "1" *--> "incident 1" IncidentReport
    HistoricalIncidentResponse "1" *--> "evidence 1" Evidence
    HistoricalIncidentResponse "1" *--> "responses *" HistoricalResponse
    HistoricalIncidentResponse "1" *--> "location 1" IncidentLocation
    HistoricalIncidentResponse "1" *--> "incident 1" IncidentReport
    HistoricalIncidentResponse "1" *--> "otherPersonInvolvement 1" OtherPersonInvolvement
    HistoricalIncidentResponse "1" *--> "prisonerInvolvement 1" PrisonerInvolvement
    HistoricalIncidentResponse "1" *--> "staffInvolvement 1" StaffInvolvement
    HistoricalResponse "1" *--> "incidentResponse 1" HistoricalIncidentResponse
    IncidentCorrectionRequest "1" *--> "reason 1" CorrectionReason
    IncidentCorrectionRequest "1" *--> "incident 1" IncidentReport
    IncidentEvent "1" *--> "incidents *" IncidentReport
    IncidentLocation "1" *--> "incident 1" IncidentReport
    IncidentReport  ..>  Evidence : «create»
    IncidentReport "1" *--> "evidence *" Evidence
    IncidentReport "1" *--> "historyOfResponses *" HistoricalIncidentResponse
    IncidentReport  ..>  IncidentCorrectionRequest : «create»
    IncidentReport "1" *--> "incidentCorrectionRequests *" IncidentCorrectionRequest
    IncidentReport "1" *--> "event 1" IncidentEvent
    IncidentReport  ..>  IncidentLocation : «create»
    IncidentReport "1" *--> "locations *" IncidentLocation
    IncidentReport "1" *--> "incidentResponses *" IncidentResponse
    IncidentReport  ..>  IncidentResponse : «create»
    IncidentReport "1" *--> "status 1" IncidentStatus
    IncidentReport "1" *--> "incidentType 1" IncidentType
    IncidentReport  ..>  OtherPersonInvolvement : «create»
    IncidentReport "1" *--> "otherPeopleInvolved *" OtherPersonInvolvement
    IncidentReport  ..>  PrisonerInvolvement : «create»
    IncidentReport "1" *--> "prisonersInvolved *" PrisonerInvolvement
    IncidentReport "1" *--> "staffInvolved *" StaffInvolvement
    IncidentReport  ..>  StaffInvolvement : «create»
    IncidentReport "1" *--> "historyOfStatuses *" StatusHistory
    IncidentResponse "1" *--> "evidence 1" Evidence
    IncidentResponse "1" *--> "location 1" IncidentLocation
    IncidentResponse "1" *--> "incident 1" IncidentReport
    IncidentResponse "1" *--> "otherPersonInvolvement 1" OtherPersonInvolvement
    IncidentResponse "1" *--> "prisonerInvolvement 1" PrisonerInvolvement
    IncidentResponse "1" *--> "responses *" Response
    IncidentResponse  ..>  Response : «create»
    IncidentResponse "1" *--> "staffInvolvement 1" StaffInvolvement
    OtherPersonInvolvement "1" *--> "incident 1" IncidentReport
    OtherPersonInvolvement "1" *--> "personType 1" PersonRole
    PrisonerInvolvement "1" *--> "incident 1" IncidentReport
    PrisonerInvolvement "1" *--> "outcome 1" PrisonerOutcome
    PrisonerInvolvement "1" *--> "prisonerInvolvement 1" PrisonerRole
    Response "1" *--> "incidentResponse 1" IncidentResponse
    StaffInvolvement "1" *--> "incident 1" IncidentReport
    StaffInvolvement "1" *--> "staffRole 1" StaffRole
    StatusHistory "1" *--> "incident 1" IncidentReport
    StatusHistory "1" *--> "status 1" IncidentStatus

```

### ER Schema
```mermaid
classDiagram
    direction BT
    class evidence {
        uuid incident_id
        text description_of_evidence
        varchar(80) type_of_evidence
        integer id
    }
    class historical_incident_response {
        uuid incident_id
        varchar(120) data_item
        text additional_information
        bigint location_id
        bigint other_person_involvement_id
        bigint prisoner_involvement_id
        bigint evidence_id
        bigint staff_involvement_id
        varchar(500) data_item_description
        integer sequence
        integer id
    }
    class historical_response {
        bigint incident_response_id
        varchar(120) item_value
        text additional_information
        timestamp recorded_on
        varchar(120) recorded_by
        integer sequence
        integer id
    }
    class incident_correction_request {
        uuid incident_id
        varchar(120) correction_requested_by
        text description_of_change
        varchar(80) reason
        timestamp correction_requested_at
        integer id
    }
    class incident_event {
        varchar(25) event_id
        varchar(3) prison_id
        timestamp event_date_and_time
        text event_details
        timestamp created_date
        varchar(120) last_modified_by
        timestamp last_modified_date
        integer id
    }
    class incident_location {
        uuid incident_id
        varchar(210) location_id
        varchar(80) location_type
        varchar(255) location_description
        integer id
    }
    class incident_report {
        varchar(25) incident_number
        varchar(255) incident_type
        varchar(255) status
        timestamp incident_date_and_time
        timestamp reported_date
        varchar(120) assigned_to
        text incident_details
        varchar(3) prison_id
        varchar(120) reported_by
        timestamp created_date
        varchar(120) last_modified_by
        timestamp last_modified_date
        varchar(5) source
        varchar(240) summary
        bigint event_id
        varchar(20) question_set_id
        uuid id
    }
    class incident_response {
        uuid incident_id
        varchar(120) data_item
        text additional_information
        bigint location_id
        bigint other_person_involvement_id
        bigint prisoner_involvement_id
        bigint evidence_id
        bigint staff_involvement_id
        varchar(500) data_item_description
        integer sequence
        integer id
    }
    class other_person_involvement {
        uuid incident_id
        varchar(255) person_name
        varchar(80) person_type
        integer id
    }
    class prisoner_involvement {
        uuid incident_id
        varchar(7) prisoner_number
        varchar(80) prisoner_involvement
        text comment
        varchar(30) outcome
        integer id
    }
    class response {
        bigint incident_response_id
        varchar(120) item_value
        text additional_information
        timestamp recorded_on
        varchar(120) recorded_by
        integer sequence
        integer id
    }
    class staff_involvement {
        uuid incident_id
        varchar(80) staff_role
        varchar(120) staff_username
        text comment
        integer id
    }
    class status_history {
        uuid incident_id
        varchar(30) status
        timestamp set_on
        varchar(120) set_by
        integer id
    }

    evidence  -->  incident_report : incident_id
    historical_incident_response  -->  evidence : evidence_id
    historical_incident_response  -->  incident_location : location_id
    historical_incident_response  -->  incident_report : incident_id
    historical_incident_response  -->  other_person_involvement : other_person_involvement_id
    historical_incident_response  -->  prisoner_involvement : prisoner_involvement_id
    historical_incident_response  -->  staff_involvement : staff_involvement_id
    historical_response  -->  historical_incident_response : incident_response_id
    historical_response  -->  incident_response : incident_response_id
    incident_correction_request  -->  incident_report : incident_id
    incident_location  -->  incident_report : incident_id
    incident_report  -->  incident_event : event_id
    incident_response  -->  evidence : evidence_id
    incident_response  -->  incident_location : location_id
    incident_response  -->  incident_report : incident_id
    incident_response  -->  other_person_involvement : other_person_involvement_id
    incident_response  -->  prisoner_involvement : prisoner_involvement_id
    incident_response  -->  staff_involvement : staff_involvement_id
    other_person_involvement  -->  incident_report : incident_id
    prisoner_involvement  -->  incident_report : incident_id
    response  -->  incident_response : incident_response_id
    staff_involvement  -->  incident_report : incident_id
    status_history  -->  incident_report : incident_id

```