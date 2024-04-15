create table incident_location
(
    id                   serial
        constraint incident_location_pk
            primary key,
    incident_id          uuid         not null
        constraint incident_location_incident_report_fk
            references incident_report,
    location_id          varchar(210) not null,
    location_type        varchar(80)  not null,
    location_description varchar(255)
);

create index incident_location_incident_report_fk_idx
    on incident_location (incident_id);


