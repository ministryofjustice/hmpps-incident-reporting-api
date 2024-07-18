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
        UUID  id
        LocalDateTime  createdAt
        String  description
        LocalDateTime  eventDateAndTime
        String  eventReference
        LocalDateTime  modifiedAt
        String  modifiedBy
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
        String  additionalInformation
        String  code
        String  question
    }
    class HistoricalResponse {
        Long  id
        String  additionalInformation
        LocalDateTime  recordedAt
        String  recordedBy
        String  response
    }
    class History {
        Long  id
        LocalDateTime  changedAt
        String  changedBy
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
        String  prisonerNumber
        PrisonerRole  prisonerRole
    }
    class Question {
        Long  id
        String  additionalInformation
        String  code
        String  question
    }
    class Report {
        UUID  id
        String  assignedTo
        LocalDateTime  createdAt
        String  description
        LocalDateTime  incidentDateAndTime
        LocalDateTime  modifiedAt
        String  modifiedBy
        String  prisonId
        String  questionSetId
        String  reportReference
        LocalDateTime  reportedAt
        String  reportedBy
        InformationSource  source
        Status  status
        String  title
        Type  type
    }
    class Response {
        Long  id
        String  additionalInformation
        LocalDateTime  recordedAt
        String  recordedBy
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
        LocalDateTime  changedAt
        String  changedBy
        Status  status
    }

CorrectionRequest "0..*" <--> "1" Report
HistoricalQuestion "0..*" <--> "1" History
HistoricalResponse "0..*" <--> "1" HistoricalQuestion
Question "1" <--> "0..*" Response
Report "1..*" <--> "1" Event
Report "1" <--> "0..*" Evidence
Report "1" <--> "0..*" History
Report "1" <--> "0..*" Location
Report "1" <--> "0..*" PrisonerInvolvement
Report "1" <--> "0..*" Question
StaffInvolvement "0..*" <--> "1" Report
StatusHistory "1..*" <--> "1" Report
```

### DB Schema

```mermaid
classDiagram
    direction BT
    class correction_request {
        uuid report_id
        varchar(60) reason
        text description_of_change
        timestamp correction_requested_at
        varchar(120) correction_requested_by
        integer id
    }
    class event {
        varchar(25) event_reference
        timestamp event_date_and_time
        varchar(255) title
        text description
        varchar(6) prison_id
        timestamp created_at
        varchar(120) modified_by
        timestamp modified_at
        uuid id
    }
    class evidence {
        uuid report_id
        varchar(60) type
        text description
        integer id
    }
    class historical_question {
        integer history_id
        integer sequence
        varchar(60) code
        text question
        text additional_information
        integer id
    }
    class historical_response {
        integer historical_question_id
        integer sequence
        text response
        text additional_information
        timestamp recorded_at
        varchar(120) recorded_by
        integer id
    }
    class history {
        uuid report_id
        varchar(60) type
        timestamp changed_at
        varchar(120) changed_by
        integer id
    }
    class location {
        uuid report_id
        varchar(60) location_id
        varchar(60) type
        text description
        integer id
    }
    class prisoner_involvement {
        uuid report_id
        varchar(7) prisoner_number
        varchar(60) prisoner_role
        varchar(60) outcome
        text comment
        integer id
    }
    class question {
        uuid report_id
        integer sequence
        varchar(60) code
        text question
        text additional_information
        integer id
    }
    class report {
        uuid event_id
        varchar(25) report_reference
        varchar(255) title
        text description
        varchar(6) prison_id
        varchar(60) type
        varchar(5) source
        varchar(60) status
        timestamp incident_date_and_time
        timestamp reported_at
        varchar(120) reported_by
        varchar(120) assigned_to
        varchar(20) question_set_id
        timestamp created_at
        varchar(120) modified_by
        timestamp modified_at
        uuid id
    }
    class response {
        integer question_id
        integer sequence
        text response
        text additional_information
        timestamp recorded_at
        varchar(120) recorded_by
        integer id
    }
    class staff_involvement {
        uuid report_id
        varchar(120) staff_username
        varchar(60) staff_role
        text comment
        integer id
    }
    class status_history {
        uuid report_id
        varchar(60) status
        timestamp changed_at
        varchar(120) changed_by
        integer id
    }

correction_request  -->  report : report_id
evidence  -->  report : report_id
historical_question  -->  history : history_id
historical_response  -->  historical_question : historical_question_id
history  -->  report : report_id
location  -->  report : report_id
prisoner_involvement  -->  report : report_id
question  -->  report : report_id
report  -->  event : event_id
response  -->  question : question_id
staff_involvement  -->  report : report_id
status_history  -->  report : report_id
```
