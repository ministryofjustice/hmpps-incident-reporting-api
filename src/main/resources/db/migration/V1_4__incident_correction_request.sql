create table incident_correction_request
(
    id                      serial
        constraint incident_correction_request_pk
            primary key,
    incident_id             uuid         not null
        constraint incident_correction_request_incident_report
            references incident_report,
    correction_requested_by varchar(120) not null,
    description_of_change   text,
    reason                  varchar(80)  not null,
    correction_requested_at timestamp    not null
);

create index incident_correction_request_incident_report_fk_idx
    on incident_correction_request (incident_id);


