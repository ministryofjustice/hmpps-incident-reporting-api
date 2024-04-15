create table historical_incident_response
(
    id                      serial
        constraint historical_incident_response_pk
            primary key,
    incident_history_id     bigint            not null
        constraint historical_incident_response_incident_history_fk
            references incident_history,
    sequence                integer default 0 not null,
    data_item               varchar(120)      not null,
    data_item_description   varchar(500),
    additional_information  text,
    location_id             bigint
        constraint historical_incident_response_location_fk
            references incident_location,
    prisoner_involvement_id bigint
        constraint historical_incident_response_prisoner_fk
            references prisoner_involvement,
    evidence_id             bigint
        constraint historical_incident_response_evidence_fk
            references evidence,
    staff_involvement_id    bigint
        constraint historical_incident_response_staff_fk
            references staff_involvement

);

create index historical_incident_response_incident_history_id_index
    on historical_incident_response (incident_history_id);

