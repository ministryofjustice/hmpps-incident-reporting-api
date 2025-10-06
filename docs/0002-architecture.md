# 2. Architecture Overview

[Next >>](0003-migration.md)


Date: 2024-02-03

## Status

Accepted

## Context

Incident Reporting System (IRS) is a new system that will be used to record incidents that occur in prisons. This document illustrates where this fits into the wider architecture.

### Components
High level set of components that would make up the flow of data around the incident reporting service.

#### Proposed Architecture
The proposed architecture for the incident reporting system (IRS) is as follows:

- **Incident Reporting API** - A RESTful API to allow the creation of incidents, viewing and managing existing incidents.
The API models new incident data seperately from the existing migrated IR data from NOMIS.
The API will receive updates from NOMIS via Syscon APIs when reports are completed.

- **Front end application** to allow staff to create, updated and view historical incident reports. Each incident type will be delivered in phases. For each support incident report that type will be removed from NOMIS, meaning that no new reports of that type can be created in NOMIS.

- **Incident database** to store incident reports and all migrated data from NOMIS

#### Other components and interactions with IRS
- Digital DataHub (DH) - The DH system will eventually be the source of truth for reporting incident data. Initially reporting will be done from NOMIS and also with the incident service.
- NOMIS - The legacy system that will be replaced by the new IRS.
- Audit Service - The audit service will receive and record audit events from the IRS.
- Analytics Platform (AP) - The analytics platform will obtain data sets from DH to provide insights and reporting.
- Safety Diagnostic Tool (SDT) - The Safety Diagnostic Tool will obtain data from AP to provide insights and reporting.
- Performance Hub (PH) - The Performance Hub will obtain data from AP to provide insights and reporting.
- SYSCON Services - These services will migrate all incident data to IRS and synchronise (2 way)

Below illustrate these components and how they interact with each other.

```mermaid
flowchart TB

subgraph prisonStaff[Prison Staff]
    h1[-Person-]:::type
    d1[RO, DW, QA staff]:::description
end
prisonStaff:::person

prisonStaff--Create/Review Incident Reports -->incidentReportApplication
prisonStaff--View MIS Reports -->oracleForms

subgraph incidentReportingService[Incident Reporting Service]
    subgraph incidentReportApplication[Incident Reporting Application]
        direction LR
        h2[Container: Node / Typescript]:::type
        d2[Workflows and incident reporting creation]:::description
    end
    incidentReportApplication:::internalContainer

    subgraph incidentReportingApi[Incident Reporting API]
        direction LR
        h5[Container: Kotlin / Spring Boot]:::type
        d5[Provides incident report\n functionality via a JSON API]:::description
    end
    incidentReportingApi:::internalContainer

    subgraph dataHubApi[Data Hub API]
        direction LR
        h25[Container: Kotlin / Node]:::type
        d25[Reporting from IR database and Redshift]:::description
    end
    dataHubApi:::internalContainer

    subgraph database[Incident Reporting Database]
        direction LR
        h6[Container: Postgres Database Schema]:::type
        d6[Stores incidents, historical types, reference data]:::description
    end
    database:::internalContainer

    incidentReportApplication--JSON calls -->incidentReportingApi
    incidentReportApplication--JSON calls -->dataHubApi
    dataHubApi--Reads -->database
    incidentReportingApi--Reads from and writes to -->database
end
incidentReportingService:::newSystem

incidentReportingApi--Publishes incidents created/updated events -->domainEvents
incidentReportingApi--Audits changes -->auditService
incidentReportApplication--Creates reports from -->dataHubApi
dataHubApi--Reads -->dataHub

subgraph ap[Analytics Platform]
    subgraph dataIngestion[Data Ingestion and Processing]
        subgraph apService[Platform Service]
            direction LR
            h418[Mixed]:::type
            d419[Ingestion and Processing of Data sets]:::description
        end
        apService:::internalContainer
    end
    dataIngestion:::internalSystem

      subgraph performanceHub[Performance Hub]
        direction LR
        h191[Container: python]:::type
        d191[Prison Performance Data]:::description
    end
    performanceHub:::legacyContainer

    subgraph sdt[Safety Diagnostic Tool]
        direction LR
        h192[Container: R]:::type
        d192[Incident Reports Tool]:::description
    end
    sdt:::legacyContainer
end

apService-- obtains data sets from -->dataHub
subgraph digitalServices[Digital Services]
    subgraph auditSystem[Audit Services]
        subgraph auditService[Audit Service]
            direction LR
            h62[Container: Kotlin / Spring Boot]:::type
            d62[Receives and records audit events]:::description
        end
        auditService:::internalContainer
    end
    auditSystem:::internalSystem

     subgraph dataHub[Digital Data hub]
         subgraph redshift[AWS Redshift Data warehouse]
             direction LR
             h41[Data warehouse]:::type
             d41[Ingestion and Processing of Data sets]:::description
         end
         redshift:::internalContainer
     end
     dataHub:::internalSystem
end

dataHub-- extracts data from NOMIS-->nomisDb
dataHub-- extracts data from Incident DB-->database

subgraph NOMIS[NOMIS Environment]

    subgraph sysconApis[Syscon Services]
        direction LR
        h82[Container: Kotlin / Spring Boot]:::type
        d82[Migration and Sync Management Services]:::description
    end
    sysconApis:::sysconContainer
    subgraph oracleForms[NOMIS front end]
        direction LR
        h91[Container: Weblogic / Oracle Forms]:::type
        d91[Incident Reports - OFF/Not Visible]:::description
    end
    oracleForms:::legacyContainer
    subgraph nomisDb[NOMIS Database]
        direction LR
        h92[Container: Oracle 11g Database]:::type
        d92[Stores core information about prisoners, prisons, finance, etc]:::description
    end
    nomisDb:::legacyContainer

    subgraph nomisMISDb[MIS Database]
        direction LR
        h93[Container: Oracle 11g Database]:::type
        d93[NOMIS Reporting System]:::description
    end
    nomisMISDb:::legacyContainer
    nomisMISDb-- extracts data from -->nomisDb
    oracleForms-- read/write data to -->nomisDb
end
NOMIS:::legacySystem
sysconApis-- 2-way sync for incident reports  -->database

subgraph otherServices[Other Services]



end
otherServices:::legacySystem

apService-- sends data to --> sdt
apService-- sends data to -->performanceHub
%% Element type definitions

classDef person fill:#90BD90, color:#000
classDef internalContainer fill:#1168bd, color:#fff
classDef legacyContainer fill:purple, color:#fff
classDef sysconContainer fill:#1168bd, color:#fff
classDef internalSystem fill:#A8B5BD
classDef newSystem fill:#D5EAF6, color:#000
classDef legacySystem fill:#A890BD, color:#fff


classDef type stroke-width:0px, color:#fff, fill:transparent, font-size:12px
classDef description stroke-width:0px, color:#fff, fill:transparent, font-size:13px
	style incidentReportingService stroke-width:2px
```

[Next >>](0003-migration.md)
