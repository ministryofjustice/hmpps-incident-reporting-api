create table response
(
    id                     serial
        constraint response_pk
            primary key,
    incident_response_id   bigint                                           not null
        constraint response_incident_response_fk
            references incident_response,
    sequence               integer      default 0                           not null,
    item_value             varchar(120)                                     not null,
    additional_information text,
    recorded_on            timestamp    default CURRENT_TIMESTAMP           not null,
    recorded_by            varchar(120) default 'system'::character varying not null
);

create index response_incident_response_fk_idx
    on response (incident_response_id);

