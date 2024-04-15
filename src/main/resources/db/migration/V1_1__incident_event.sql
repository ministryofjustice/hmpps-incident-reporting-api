create table incident_event
(
    id                  serial
        constraint incident_event_pk
            primary key,
    event_id            varchar(25)  not null,
    prison_id           varchar(3)   not null,
    event_date_and_time timestamp    not null,
    event_details       text         not null,
    created_date        timestamp    not null,
    last_modified_by    varchar(120) not null,
    last_modified_date  timestamp    not null
);

create unique index incident_event_id_idx
    on incident_event (event_id);

create sequence incident_event_sequence
    start with 1000000;
