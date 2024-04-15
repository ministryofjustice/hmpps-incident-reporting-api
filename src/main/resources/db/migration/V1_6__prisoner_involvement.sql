create table prisoner_involvement
(
    id                   serial
        constraint prisoner_involvement_pk
            primary key,
    incident_id          uuid        not null
        constraint prisoner_involvement_report_fk
            references incident_report,
    prisoner_number      varchar(7)  not null,
    prisoner_involvement varchar(80) not null,
    comment              text,
    outcome              varchar(30)
);

create index prisoner_involvement_incident_report_fk_idx
    on prisoner_involvement (incident_id);

