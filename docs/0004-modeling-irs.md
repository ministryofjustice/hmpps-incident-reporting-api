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
    class Evidence {
        String descriptionOfEvidence
        Long? id
        String typeOfEvidence
        IncidentReport incident
    }
    class HistoricalIncidentResponse {
        String comment
        Question question
        IncidentLocation? location
        PrisonerInvolvement? prisonerInvolvement
        Long? id
        StaffInvolvement? staffInvolvement
        String recordedBy
        List~HistoricalResponse~ responses
        OtherPersonInvolvement? otherPersonInvolvement
        Evidence? evidence
        LocalDateTime recordedOn
        IncidentReport incident
    }
    class HistoricalResponse {
        ResponseOption response
        Long? id
        HistoricalIncidentResponse incidentResponse
        String? moreInfo
    }
    class IncidentCorrectionRequest {
        String reason
        String correctionRequestedBy
        String descriptionOfChange
        Long? id
        IncidentReport incident
        LocalDateTime correctionRequestedAt
    }
    class IncidentLocation {
        String locationType
        String locationId
        Long? id
        IncidentReport incident
        String? locationDescription
    }
    class IncidentReport {
        QuestionSet questionSetUsed
        String prisonId
        List~Evidence~ evidence
        List~IncidentResponse~ incidentResponses
        List~HistoricalIncidentResponse~ historyOfResponses
        List~PrisonerInvolvement~ prisonersInvolved
        String assignedTo
        LocalDateTime reportedDate
        String incidentDetails
        UUID? id
        List~IncidentLocation~ locations
        String reportedBy
        String lastModifiedBy
        LocalDateTime lastModifiedDate
        List~StatusHistory~ historyOfStatuses
        LocalDateTime createdDate
        List~IncidentCorrectionRequest~ incidentCorrectionRequests
        LocalDateTime incidentDateAndTime
        List~StaffInvolvement~ staffInvolved
        IncidentStatus status
        IncidentType incidentType
        String incidentNumber
        List~OtherPersonInvolvement~ otherPeopleInvolved
    }
    class IncidentReportRepository {
        <<Interface>>

    }
    class IncidentResponse {
        String? comment
        Question question
        IncidentLocation? location
        PrisonerInvolvement? prisonerInvolvement
        Long? id
        StaffInvolvement? staffInvolvement
        String recordedBy
        List~Response~ responses
        OtherPersonInvolvement? otherPersonInvolvement
        Evidence? evidence
        LocalDateTime recordedOn
        IncidentReport incident
    }
    class IncidentStatus {
        <<enumeration>>
        EnumEntries~IncidentStatus~ entries
    }
    class IncidentType {
        <<enumeration>>
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
        Long? id
        IncidentReport incident
    }
    class PrisonerRole {
        <<enumeration>>
        EnumEntries~PrisonerRole~ entries
    }
    class Question {
        <<enumeration>>
        EnumEntries~Question~ entries
    }
    class QuestionSet {
        <<enumeration>>
        EnumEntries~QuestionSet~ entries
    }
    class Response {
        ResponseOption response
        Long? id
        IncidentResponse incidentResponse
        String? moreInfo
    }
    class ResponseOption {
        <<enumeration>>
        EnumEntries~ResponseOption~ entries
    }
    class StaffInvolvement {
        StaffRole staffRole
        Long? id
        String staffUsername
        IncidentReport incident
    }
    class StaffRole {
        <<enumeration>>
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
    HistoricalIncidentResponse "1" *--> "question 1" Question
    HistoricalIncidentResponse "1" *--> "staffInvolvement 1" StaffInvolvement
    HistoricalResponse "1" *--> "incidentResponse 1" HistoricalIncidentResponse
    HistoricalResponse "1" *--> "response 1" ResponseOption
    IncidentCorrectionRequest "1" *--> "incident 1" IncidentReport
    IncidentLocation "1" *--> "incident 1" IncidentReport
    IncidentReport  ..>  Evidence : «create»
    IncidentReport "1" *--> "evidence *" Evidence
    IncidentReport "1" *--> "historyOfResponses *" HistoricalIncidentResponse
    IncidentReport "1" *--> "incidentCorrectionRequests *" IncidentCorrectionRequest
    IncidentReport "1" *--> "locations *" IncidentLocation
    IncidentReport  ..>  IncidentLocation : «create»
    IncidentReport  ..>  IncidentResponse : «create»
    IncidentReport "1" *--> "incidentResponses *" IncidentResponse
    IncidentReport "1" *--> "status 1" IncidentStatus
    IncidentReport "1" *--> "incidentType 1" IncidentType
    IncidentReport "1" *--> "otherPeopleInvolved *" OtherPersonInvolvement
    IncidentReport  ..>  OtherPersonInvolvement : «create»
    IncidentReport "1" *--> "prisonersInvolved *" PrisonerInvolvement
    IncidentReport  ..>  PrisonerInvolvement : «create»
    IncidentReport "1" *--> "questionSetUsed 1" QuestionSet
    IncidentReport "1" *--> "staffInvolved *" StaffInvolvement
    IncidentReport  ..>  StaffInvolvement : «create»
    IncidentReport "1" *--> "historyOfStatuses *" StatusHistory
    IncidentResponse "1" *--> "evidence 1" Evidence
    IncidentResponse "1" *--> "location 1" IncidentLocation
    IncidentResponse "1" *--> "incident 1" IncidentReport
    IncidentResponse "1" *--> "otherPersonInvolvement 1" OtherPersonInvolvement
    IncidentResponse "1" *--> "prisonerInvolvement 1" PrisonerInvolvement
    IncidentResponse "1" *--> "question 1" Question
    IncidentResponse "1" *--> "responses *" Response
    IncidentResponse  ..>  Response : «create»
    IncidentResponse "1" *--> "staffInvolvement 1" StaffInvolvement
    OtherPersonInvolvement "1" *--> "incident 1" IncidentReport
    OtherPersonInvolvement "1" *--> "personType 1" PersonRole
    PrisonerInvolvement "1" *--> "incident 1" IncidentReport
    PrisonerInvolvement "1" *--> "prisonerInvolvement 1" PrisonerRole
    Response "1" *--> "incidentResponse 1" IncidentResponse
    Response "1" *--> "response 1" ResponseOption
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
        varchar(120) question
        text comment
        bigint location_id
        bigint other_person_involvement_id
        bigint prisoner_involvement_id
        bigint evidence_id
        bigint staff_involvement_id
        timestamp recorded_on
        varchar(120) recorded_by
        integer id
    }
    class historical_response {
        bigint incident_response_id
        varchar(120) response
        text more_info
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
    class incident_location {
        uuid incident_id
        varchar(210) location_id
        varchar(80) location_type
        varchar(255) location_description
        integer id
    }
    class incident_report {
        varchar(10) incident_number
        varchar(255) incident_type
        varchar(255) status
        timestamp incident_date_and_time
        timestamp reported_date
        varchar(120) assigned_to
        text incident_details
        varchar(3) prison_id
        varchar(80) question_set_used
        varchar(120) reported_by
        timestamp created_date
        varchar(120) last_modified_by
        timestamp last_modified_date
        uuid id
    }
    class incident_response {
        uuid incident_id
        varchar(120) question
        text comment
        bigint location_id
        bigint other_person_involvement_id
        bigint prisoner_involvement_id
        bigint evidence_id
        bigint staff_involvement_id
        timestamp recorded_on
        varchar(120) recorded_by
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
        integer id
    }
    class response {
        bigint incident_response_id
        varchar(120) response
        text more_info
        integer id
    }
    class staff_involvement {
        uuid incident_id
        varchar(80) staff_role
        varchar(120) staff_username
        integer id
    }
    class status_history {
        uuid incident_id
        timestamp set_on
        varchar(120) set_by
        varchar(30) status
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