create table evidence
(
    id                      serial
        constraint evidence_pk
            primary key,
    incident_id             uuid        not null
        constraint evidence_incident_report_fk
            references incident_report,
    description_of_evidence text        not null,
    type_of_evidence        varchar(80) not null
);

create index evidence_incident_report_fk_idx
    on evidence (incident_id);


