# Key Features: Incident Reporting API

The HMPPS Incident Reporting API is the central service for managing incident reports within prisons. It supports the full lifecycle of an incident, from initial reporting to structured data collection, review, and synchronization with legacy systems (NOMIS).

## 1. Incident Report Lifecycle

The core API allows for creating, retrieving, and updating incident reports.

### Creating a Report
- **`POST /incident-reports`**: Create a new draft incident report.
  - **Required fields**: `type` (e.g., `ASSAULT_1`), `incidentDateAndTime`, `location` (prison ID), `title`, and `description`.
  - Reports are initially created in `DRAFT` status.

### Editing a Report
- **`PATCH /incident-reports/{id}`**: Update basic properties of an incident report.
  - Can update `title`, `description`, `location`, and `incidentDateAndTime`.
  - Also used to mark whether prisoner or staff involvement data entry is complete (`prisonerInvolvementDone`, `staffInvolvementDone`).
- **`PUT /incident-reports/{id}/status`**: Transition a report through its lifecycle (e.g., `DRAFT` -> `AWAITING_ANALYSIS` -> `CLOSED`).
- **`PUT /incident-reports/{id}/type`**: Update the incident type if it was incorrectly categorized. This action creates a historical version of the report.

### Retrieving Reports
- **`GET /incident-reports`**: Search and filter basic incident reports by location, type, status, dates, and involved persons.
- **`GET /incident-reports/{id}/details`**: Retrieve the full details of a specific report, including all involved parties, question responses, and history.

## 2. Key Management Endpoints

Beyond basic report details, the API provides several specialized endpoints for comprehensive incident management:

- **Involved Parties**:
  - `POST /incident-reports/{reportId}/prisoners-involved`: Add a prisoner to an incident, specifying their role and outcome.
  - `POST /incident-reports/{reportId}/staff-involved`: Add a staff member involved in the incident.
- **Structured Data (Questions & Responses)**:
  - `PUT /incident-reports/{reportId}/questions`: Add or update responses to specific questions required for the incident type.
- **Corrections and Addendums**:
  - `POST /incident-reports/{reportId}/correction-requests`: Managers can request corrections or clarifications from the reporter.
  - `POST /incident-reports/{reportId}/description-addendums`: Add supplementary information to the main incident description without altering the original record.
- **Synchronization**:
  - `POST /sync/upsert`: Bi-directional synchronization with NOMIS.

## 3. Reference Data (Constants)

The API provides metadata endpoints to ensure clients use valid values for various fields:

- `GET /constants/types`: All supported incident types (e.g., `ASSAULT_1`, `DRUGS_1`).
- `GET /constants/statuses`: All valid report statuses (`DRAFT`, `AWAITING_ANALYSIS`, `CLOSED`, etc.).
- `GET /constants/prisoner-roles` & `GET /constants/staff-roles`: Roles for persons involved (e.g., `VICTIM`, `OFFENDER`, `WITNESS`).
- `GET /constants/prisoner-outcomes`: Outcomes of a prisoner's involvement.
- `GET /constants/information-sources`: Valid sources of report data (e.g., `DPS`, `NOMIS`).
- `GET /constants/user-actions`: Valid user actions from correction requests.
- `GET /constants/error-codes`: Application error codes.

## 4. Domain Structure

An incident report is composed of several core entities:

- **Report**: The primary entity containing basic metadata (reference, type, status, location, dates, etc.).
- **StaffInvolvement**: Links staff members to a report with specific roles (e.g., `REPORTING_OFFICER`).
- **PrisonerInvolvement**: Links prisoners to a report with roles (e.g., `OFFENDER`) and outcomes (e.g., `PLACED_IN_SEGREGATION`).
- **Question & Response**: A flexible structure for capturing detailed information specific to an incident type.
- **CorrectionRequest**: A record of a manager's request for the reporter to amend the report.
- **DescriptionAddendum**: Supplementary text added to the report after it has been initially submitted.
- **History**: Historical snapshots of a report, preserved when major changes (like type) occur.

```
