# 4. A new modelling for the Incident Reporting Service

[Next >>](9999-end.md)


Date: 2024-03-04

## Status

Accepted

## Context
This document details the new design of Incident Reports that will support old NOMIS model and a new more flexible model for the Incident Reporting Service.

### ER Schema

```mermaid
classDiagram
    direction BT
    class CorrectionRequest {
        Long  id
        LocalDateTime  correctionRequestedAt
        String  correctionRequestedBy
        String  descriptionOfChange
        CorrectionReason  reason
    }
    class Event {
        Long  id
        LocalDateTime  createdDate
        String  description
        LocalDateTime  eventDateAndTime
        String  eventId
        String  lastModifiedBy
        LocalDateTime  lastModifiedDate
        String  prisonId
        String  title
    }
    class Evidence {
        Long  id
        String  description
        String  type
    }
    class HistoricalQuestion {
        Long  id
        String  code
        String  question
    }
    class HistoricalResponse {
        Long  id
        String  additionalInformation
        String  recordedBy
        LocalDateTime  recordedOn
        String  response
    }
    class History {
        Long  id
        LocalDateTime  changeDate
        String  changeStaffUsername
        Type  type
    }
    class Location {
        Long  id
        String  description
        String  locationId
        String  type
    }
    class PrisonerInvolvement {
        Long  id
        String  comment
        PrisonerOutcome  outcome
        PrisonerRole  prisonerInvolvement
        String  prisonerNumber
    }
    class Question {
        Long  id
        String  code
        String  question
    }
    class Report {
        UUID  id
        String  assignedTo
        LocalDateTime  createdDate
        String  description
        LocalDateTime  incidentDateAndTime
        String  incidentNumber
        String  lastModifiedBy
        LocalDateTime  lastModifiedDate
        String  prisonId
        String  questionSetId
        String  reportedBy
        LocalDateTime  reportedDate
        InformationSource  source
        Status  status
        String  title
        Type  type
    }
    class Response {
        Long  id
        String  additionalInformation
        String  recordedBy
        LocalDateTime  recordedOn
        String  response
    }
    class StaffInvolvement {
        Long  id
        String  comment
        StaffRole  staffRole
        String  staffUsername
    }
    class StatusHistory {
        Long  id
        String  setBy
        LocalDateTime  setOn
        Status  status
    }

CorrectionRequest "1..*" <--> "1" Report
Evidence "1..*" <--> "1" Report
HistoricalQuestion "0..1" --> "0..1" Evidence
HistoricalQuestion "0..1" --> "0..*" HistoricalResponse
HistoricalQuestion "1..*" <--> "1" History
HistoricalQuestion "0..1" --> "0..1" Location
HistoricalQuestion "0..1" --> "0..1" PrisonerInvolvement
HistoricalQuestion "0..1" --> "0..1" StaffInvolvement
Question "0..1" --> "0..1" Evidence
Question "0..1" --> "0..1" Location
Question "0..1" --> "0..1" PrisonerInvolvement
Question "0..1" --> "0..*" Response
Question "0..1" --> "0..1" StaffInvolvement
Report "1..*" <--> "1" Event
Report "1" <--> "1..*" History
Report "1" <--> "1..*" Location
Report "1" <--> "1..*" PrisonerInvolvement
Report "1" <--> "1..*" Question
StaffInvolvement "1..*" <--> "1" Report
StatusHistory "1..*" <--> "1" Report
```

### DB Schema

```mermaid
classDiagram
    direction BT
    class correction_request {
       uuid report_id
       varchar(120) correction_requested_by
       text description_of_change
       varchar(80) reason
       timestamp correction_requested_at
       integer id
    }
    class event {
       varchar(25) event_id
       varchar(6) prison_id
       timestamp event_date_and_time
       varchar(255) title
       text description
       timestamp created_date
       varchar(120) last_modified_by
       timestamp last_modified_date
       integer id
    }
    class evidence {
       uuid report_id
       varchar(80) type
       text description
       integer id
    }
    class historical_question {
       bigint history_id
       integer sequence
       varchar(120) code
       text question
       text additional_information
       bigint location_id
       bigint prisoner_involvement_id
       bigint evidence_id
       bigint staff_involvement_id
       integer id
    }
    class historical_response {
       bigint historical_question_id
       integer sequence
       text response
       text additional_information
       timestamp recorded_on
       varchar(120) recorded_by
       integer id
    }
    class history {
       uuid report_id
       varchar(255) type
       timestamp change_date
       varchar(120) change_staff_username
       integer id
    }
    class location {
       uuid report_id
       varchar(210) location_id
       varchar(80) type
       text description
       integer id
    }
    class prisoner_involvement {
       uuid report_id
       varchar(7) prisoner_number
       varchar(80) prisoner_involvement
       text comment
       varchar(30) outcome
       integer id
    }
    class question {
       uuid report_id
       integer sequence
       varchar(120) code
       text question
       text additional_information
       bigint location_id
       bigint prisoner_involvement_id
       bigint evidence_id
       bigint staff_involvement_id
       integer id
    }
    class report {
       bigint event_id
       varchar(25) incident_number
       varchar(255) type
       varchar(20) question_set_id
       varchar(255) status
       timestamp incident_date_and_time
       varchar(6) prison_id
       timestamp reported_date
       varchar(120) assigned_to
       varchar(255) title
       text description
       varchar(120) reported_by
       timestamp created_date
       varchar(5) source
       varchar(120) last_modified_by
       timestamp last_modified_date
       uuid id
    }
    class response {
       bigint question_id
       integer sequence
       text response
       text additional_information
       timestamp recorded_on
       varchar(120) recorded_by
       integer id
    }
    class staff_involvement {
       uuid report_id
       varchar(80) staff_role
       varchar(120) staff_username
       text comment
       integer id
    }
    class status_history {
       uuid report_id
       varchar(30) status
       timestamp set_on
       varchar(120) set_by
       integer id
    }

correction_request  -->  report : report_id
evidence  -->  report : report_id
historical_question  -->  evidence : evidence_id
historical_question  -->  history : history_id
historical_question  -->  location : location_id
historical_question  -->  prisoner_involvement : prisoner_involvement_id
historical_question  -->  staff_involvement : staff_involvement_id
historical_response  -->  historical_question : historical_question_id
history  -->  report : report_id
location  -->  report : report_id
prisoner_involvement  -->  report : report_id
question  -->  evidence : evidence_id
question  -->  location : location_id
question  -->  prisoner_involvement : prisoner_involvement_id
question  -->  report : report_id
question  -->  staff_involvement : staff_involvement_id
report  -->  event : event_id
response  -->  question : question_id
staff_involvement  -->  report : report_id
status_history  -->  report : report_id
```
