# 1. Mastering and synchronisation of Internal location data with NOMIS

[Next >>](0004-location-db-design.md)


Date: 2024-01-23

## Status

Accepted

## Context

This document will cover the approach for the migration of the "Incident Reporting Service" from NOMIS to a new service and how data will be maintained and made available to down-stream systems.
### Migration plan for "Incident Reporting Service" 

Incident reports will **not** be kept in sync and **no** data will be written back to NOMIS. 
There will be a one-off migration of all completed incidents from NOMIS to the new service.

The proposed approach will be :-
* Migrate all incident reporting data from NOMIS to the new service in a one of process. These records will be role protected (mainly read-only for most users) and will be used for historical reporting and analysis. Records will be read only and can only be "edited" in NOMIS.
* In NOMIS, switch off the ability to create new incidents of agreed types. E.g. Self harm, Assaults and Finds. No new incidents of these types will be able to be created in NOMIS. Existing records can still be edited.
* Incident types can be removed on a per prison basis if needed (although not recommended), for example a set of prisons can only create new incidents (of certain types) in the new service (IRS). Other prisons can continue to use NOMIS for these types.
* SYSCON will need to modify the incident reporting screens in NOMIS to be "edit only" for certain incident types on a per-prison basis. Config to drive this process from a screen will be required.
* Existing reports created in NOMIS in progress it will be kept in sync to the new IRS.
* All records originating in NOMIS will be viewable but not be editable in the new service.
* New incidents data will be ingested into the DPR system for reporting and down-steam applications to consume.  It is suggested that DPR takes all data from the new IRS system which includes the old (from NOMIS) report and new reports combined into a over-aching dataset. This will allow reports ported from NOMIS to show data from both old and new data.
* Existing NOMIS reports will need to be migrated to DPR - reports should ideally be rationalised for the new service.
* As existing reports can be altered for many years, there will be a hard cut-off of XX months on how long records can be edited in NOMIS. After this, a backend admin screen will allow changes to NOMIS created reports to be made in the new IRS service only.
* The migration process will contain all the incident data, included the question and answers used at the time and all historical changes made to that incident report.
* The migrated data will be available for searching and viewing in the new service but will be generally read only. (except where mentioned above for special circumstances).
* The new service will be the source of truth for all new incidents and will be the only place to create new incidents (of the types defined).
* The DPR system will also contain all the NOMIS incident reporting data and be used by the analytics platform, at a future point this data needs to be synchronised from the new service.
* These downstream systems will need to take incident data from two models (both in DPR) i.e. new incidents from the new service and historical incidents from NOMIS.
* SDT and performance hub for reporting and analysis will take their data from AP (analytics platform) but could at some future point pivot to use DPR.
* Over-time all other incident types will be moved to the new service and creation removed from NOMIS.
* Once all incident types for all prisons are support in new system and reports migrated to DPR the NOMIS incident screens will be removed and the NOMIS -> DPS “trickle” will be stopped.
* Finally, all data will be removed from NOMIS, code removed and tables dropped.

Steps needed:
**Incident reporting team.**
- Model the Incident Reporting data in a new database, 
- Model the legacy data from NOMIS for migration and historical viewing.
- Develop front end application for managing incidents and viewing historical incidents. Allow switch on per prison and type.
- Build API functionality for managing both data sets
- Raise events on creation or amendments of incidents
- Provide API endpoint for consuming incidents to replace Prison API endpoint used by CatTool.  This should combine both legacy and new data models.
- Build "migrate" endpoint to allow NOMIS to send incident report data to API, this will be a continuous 1 way sync whilst edit of old reports is allowed.
- Setup pipelines to DPR to allow new incidents to be ingested into this system.

**SYSCON team**
- Migrate all incidents in NOMIS by calling API "migrate" endpoint, continue to drip feed these as they change.
- Change IRS screen in NOMIS to **edit only** mode for certain incident types in defined prisons.
- Remove all incident endpoints in prison-api once all services are using new API.
- Remove reports, screens, data, code and tables once all incident types migrated and reports are replaced


## Key components and their flow for incident reporting data
```mermaid
    
graph TB
    X((Prison Staff)) --> IRS
    IRS[Incident Reporting Service] -- update IRS --> IRSAPI
    IRSAPI[Incident Reporting API] -- Store locations --> IRSDB[[Incident Reporting DB]]
    IRSAPI -- Incident Report Created/Updated --> DOM_EVT[[Domain Events]]
    X -- Create Incident Reports --> NOMIS[NOMIS]
    NOMIS -- Incident Report Created/Updated event--> E[HMPPS NOMIS -> DPS Sync]
    E -- Sync Incident Report --> IRSAPI
    F[hmpps-nomis-prisoner-api] -- get IRS details --> NOMIS
    R[HMPPS Prisoner from NOMIS Migration] -- perform migration --> IRSAPI
    R -- record history --> H[[History Record DB]]
    K[HMPPS NOMIS Mapping Service] --> Q[[Mapping DB]]
    R -- check for existing mapping --> K
    R -- 1. find out how many to migrate, 2 get IR details --> F
    AP[Analytics Platform] -- get data --> DPR[Digital Prison Reporting]
    AP -- send data --> SDT[Safety Diagnostic Tool]
    AP -- send data --> PH[Performance Hub]
    DPR -- ingest data from --> IRSDB
    DPR -- ingest data from --> NOMIS
    
```




[Next >>](9999-end.md)
