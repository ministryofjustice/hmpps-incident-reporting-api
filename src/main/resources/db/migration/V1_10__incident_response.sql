create table incident_response
(
    id                      serial
        constraint incident_response_pk
            primary key,
    incident_id             uuid              not null
        constraint incident_response_report_fk
            references incident_report,
    sequence                integer default 0 not null,
    data_item               varchar(120),
    additional_information  text,
    location_id             bigint
        constraint incident_response_location_fk
            references incident_location,
    prisoner_involvement_id bigint
        constraint incident_response_prisoner_fk
            references prisoner_involvement,
    evidence_id             bigint
        constraint incident_response_evidence_fk
            references evidence,
    staff_involvement_id    bigint
        constraint incident_response_staff_fk
            references staff_involvement,
    data_item_description   varchar(500)
);

create index incident_response_incident_fk_idx
    on incident_response (incident_id);

