create table status_history
(
    id          serial
        constraint status_history_pk
            primary key,
    incident_id uuid         not null
        constraint status_history_report_fk
            references incident_report,
    status      varchar(30)  not null,
    set_on      timestamp    not null,
    set_by      varchar(120) not null
);

create index status_history_incident_report_fk_idx
    on status_history (incident_id);

