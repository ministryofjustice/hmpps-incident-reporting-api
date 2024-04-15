create table incident_report
(
    id                     uuid       default gen_random_uuid()        not null
        constraint incident_report_pk
            primary key,
    event_id               bigint                                      not null
        constraint incident_report_event_fk
            references incident_event,
    incident_number        varchar(25)                                 not null
        constraint incident_report_uk
            unique,
    incident_type          varchar(255)                                not null,
    question_set_id        varchar(20),
    status                 varchar(255)                                not null,
    incident_date_and_time timestamp                                   not null,
    prison_id              varchar(3)                                  not null,
    reported_date          timestamp                                   not null,
    assigned_to            varchar(120)                                not null,
    summary                varchar(240),
    incident_details       text                                        not null,
    reported_by            varchar(120)                                not null,
    created_date           timestamp                                   not null,
    source                 varchar(5) default 'DPS'::character varying not null,
    last_modified_by       varchar(120)                                not null,
    last_modified_date     timestamp                                   not null
);

create unique index incident_report_incident_number_idx
    on incident_report (incident_number);

create index incident_report_incident_report_fk_idx
    on incident_report (event_id);

create sequence incident_number_sequence
    start with 1000000;
