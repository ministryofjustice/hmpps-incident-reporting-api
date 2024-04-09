create table incident_history
(
    id                   serial       not null
        constraint incident_history_pk primary key,
    incident_id          UUID         not null,
    incident_type   varchar(255) not null,
    incident_change_date   timestamp    not null,
    incident_change_staff_username varchar(120) not null
);

alter table if exists incident_history
add constraint incident_history_incident_report_fk foreign key (incident_id) references incident_report;

ALTER TABLE historical_incident_response ADD COLUMN incident_history_id bigint;
alter table historical_incident_response alter column incident_history_id set not null;

alter table historical_incident_response
    add constraint historical_incident_response_incident_history_fk foreign key (incident_history_id) references incident_history;

ALTER TABLE historical_incident_response DROP COLUMN incident_id;

CREATE INDEX incident_history_incident_id_index
    ON incident_history (incident_id);

CREATE INDEX historical_incident_response_incident_history_id_index
    ON historical_incident_response (incident_history_id);

CREATE INDEX historical_response_incident_response_id_index
    ON historical_response (incident_response_id);
