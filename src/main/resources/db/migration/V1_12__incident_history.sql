create table incident_history
(
    id                             serial
        constraint incident_history_pk
            primary key,
    incident_id                    uuid         not null
        constraint incident_history_incident_report_fk
            references incident_report,
    incident_type                  varchar(255) not null,
    incident_change_date           timestamp    not null,
    incident_change_staff_username varchar(120) not null
);

create index incident_history_incident_id_index
    on incident_history (incident_id);

