create table staff_involvement
(
    id             serial
        constraint staff_involvement_pk
            primary key,
    incident_id    uuid         not null
        constraint staff_involvement_report_fk
            references incident_report,
    staff_role     varchar(80)  not null,
    staff_username varchar(120) not null,
    comment        text
);

create index staff_involvement_incident_report_fk_idx
    on staff_involvement (incident_id);

