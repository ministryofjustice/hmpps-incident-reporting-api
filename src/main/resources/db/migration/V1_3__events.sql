CREATE TABLE incident_event
(
    id                  serial       not null
        constraint incident_event_pk primary key,
    event_id            varchar(25)  NOT NULL,
    prison_id           varchar(3)   not null,
    event_date_and_time timestamp    not null,
    summary             varchar(500) null,
    event_details       text         not null,
    created_date        timestamp    not null,
    last_modified_by    varchar(120) not null,
    last_modified_date  timestamp    not null
);

CREATE SEQUENCE incident_event_sequence INCREMENT 1 START 1000000;
create unique index incident_event_id_idx on incident_event (event_id);

alter table incident_report add column summary varchar(240);
alter table incident_report add column event_id bigint;

alter table incident_report
    add constraint incident_report_event_fk
        foreign key (event_id) references incident_event (id);