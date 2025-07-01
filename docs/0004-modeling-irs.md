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
    Long id
    LocalDateTime correctionRequestedAt
    String correctionRequestedBy
    String descriptionOfChange
    String location
    int sequence
  }
  class DescriptionAddendum {
    Long id
    LocalDateTime createdAt
    String createdBy
    String firstName
    String lastName
    int sequence
    String text
  }
  class HistoricalQuestion {
    Long id
    String additionalInformation
    String code
    String question
    int sequence
  }
  class HistoricalResponse {
    Long id
    String additionalInformation
    LocalDateTime recordedAt
    String recordedBy
    String response
    LocalDate responseDate
    int sequence
  }
  class History {
    Long id
    LocalDateTime changedAt
    String changedBy
    Type type
  }
  class PrisonerInvolvement {
    Long id
    String comment
    String firstName
    String lastName
    PrisonerOutcome outcome
    String prisonerNumber
    PrisonerRole prisonerRole
    int sequence
  }
  class Question {
    Long id
    String additionalInformation
    String code
    String question
    int sequence
  }
  class Report {
    UUID id
    LocalDateTime createdAt
    String description
    LocalDateTime incidentDateAndTime
    String location
    LocalDateTime modifiedAt
    String modifiedBy
    InformationSource modifiedIn
    boolean prisonerInvolvementDone
    String questionSetId
    String reportReference
    LocalDateTime reportedAt
    String reportedBy
    InformationSource source
    boolean staffInvolvementDone
    Status status
    String title
    Type type
  }
  class Response {
    Long id
    String additionalInformation
    LocalDateTime recordedAt
    String recordedBy
    String response
    LocalDate responseDate
    int sequence
  }
  class StaffInvolvement {
    Long id
    String comment
    String firstName
    String lastName
    int sequence
    StaffRole staffRole
    String staffUsername
  }
  class StatusHistory {
    Long id
    LocalDateTime changedAt
    String changedBy
    Status status
  }

  CorrectionRequest "0..*" <--> "1" Report
  HistoricalQuestion "1" <--> "1..*" HistoricalResponse
  HistoricalQuestion "0..*" <--> "1" History
  PrisonerInvolvement "0..*" <--> "1" Report
  Question "0..*" <--> "1" Report
  Question "1" <--> "1..*" Response
  Report "1" <--> "0..*" DescriptionAddendum
  Report "1" <--> "0..*" History
  Report "1" <--> "0..*" StaffInvolvement
  StatusHistory "1..*" <--> "1" Report
```

### DB Schema

```mermaid
classDiagram
  direction BT
  class constant_prisoner_outcome {
    integer sequence
    varchar(60) description
    varchar(60) code
  }
  class constant_prisoner_role {
    integer sequence
    varchar(60) description
    varchar(60) code
  }
  class constant_staff_role {
    integer sequence
    varchar(60) description
    varchar(60) code
  }
  class constant_status {
    integer sequence
    varchar(60) description
    varchar(60) code
  }
  class constant_type {
    integer sequence
    varchar(60) description
    boolean active
    varchar(60) family_code
    varchar(60) code
  }
  class constant_type_family {
    integer sequence
    varchar(60) description
    varchar(60) code
  }
  class correction_request {
    uuid report_id
    text description_of_change
    timestamp correction_requested_at
    varchar(120) correction_requested_by
    integer sequence
    varchar(20) location
    integer id
  }
  class description_addendum {
    uuid report_id
    integer sequence
    text text
    timestamp created_at
    varchar(120) created_by
    varchar(255) first_name
    varchar(255) last_name
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
    date response_date
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
  class prisoner_involvement {
    uuid report_id
    varchar(7) prisoner_number
    varchar(60) prisoner_role
    varchar(60) outcome
    text comment
    integer sequence
    varchar(255) first_name
    varchar(255) last_name
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
    varchar(25) report_reference
    varchar(255) title
    text description
    varchar(20) location
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
    varchar(5) modified_in
    boolean staff_involvement_done
    boolean prisoner_involvement_done
    uuid id
  }
  class response {
    integer question_id
    integer sequence
    text response
    date response_date
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
    integer sequence
    varchar(255) first_name
    varchar(255) last_name
    integer id
  }
  class status_history {
    uuid report_id
    varchar(60) status
    timestamp changed_at
    varchar(120) changed_by
    integer id
  }

  correction_request --> report: report_id
  description_addendum --> report: report_id
  historical_question --> history: history_id
  historical_response --> historical_question: historical_question_id
  history --> report: report_id
  prisoner_involvement --> report: report_id
  question --> report: report_id
  response --> question: question_id
  staff_involvement --> report: report_id
  status_history --> report: report_id
```
