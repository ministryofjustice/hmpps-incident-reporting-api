create table historical_response
(
    id                     serial
        constraint historical_response_pk
            primary key,
    incident_response_id   bigint                                           not null
        constraint historical_response_incident_fk
            references historical_incident_response,
    sequence               integer      default 0                           not null,
    item_value             varchar(120)                                     not null,
    additional_information text,
    recorded_on            timestamp    default CURRENT_TIMESTAMP           not null,
    recorded_by            varchar(120) default 'system'::character varying not null
);

create index historical_response_incident_response_id_index
    on historical_response (incident_response_id);

