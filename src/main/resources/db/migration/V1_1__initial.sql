create table incident_report
(
    id                     uuid         not null
        constraint incident_report_pk primary key DEFAULT gen_random_uuid(),
    incident_number        varchar(10)  not null
        constraint incident_report_uk unique,
    incident_type          varchar(255) not null,
    status                 varchar(255) not null,
    incident_date_and_time timestamp    not null,
    reported_date          timestamp    not null,
    assigned_to            varchar(120) not null,
    incident_details       text         not null,
    prison_id              varchar(3)   not null,
    reported_by            varchar(120) not null,
    created_date           timestamp    not null,
    last_modified_by       varchar(120) not null,
    last_modified_date     timestamp    not null
);

create table incident_location
(
    id                   serial       not null
        constraint incident_location_pk primary key,
    incident_id          UUID         not null,
    location_id          varchar(210) not null,
    location_type        varchar(80)  not null,
    location_description varchar(255)
);

alter table if exists incident_location
    add constraint incident_location_incident_report_fk foreign key (incident_id) references incident_report;

create table evidence
(
    id                      serial      not null
        constraint evidence_pk primary key,
    incident_id             UUID        not null,
    description_of_evidence text        not null,
    type_of_evidence        varchar(80) not null
);
alter table if exists evidence
    add constraint evidence_incident_report_fk foreign key (incident_id) references incident_report;

create table incident_correction_request
(
    id                      serial       not null
        constraint incident_correction_request_pk primary key,
    incident_id             UUID         not null,
    correction_requested_by varchar(120) not null,
    description_of_change   text,
    reason                  varchar(80)  not null,
    correction_requested_at timestamp    not null
);

alter table if exists incident_correction_request
    add constraint incident_correction_request_incident_report foreign key (incident_id) references incident_report;

create table incident_response
(
    id                          serial       not null
        constraint incident_response_pk primary key,
    incident_id                 UUID         not null,
    data_point_key              varchar(120),
    comment                     text,
    location_id                 bigint,
    other_person_involvement_id bigint,
    prisoner_involvement_id     bigint,
    evidence_id                 bigint,
    staff_involvement_id        bigint,
    recorded_on                 timestamp    not null,
    recorded_by                 varchar(120) not null
);

alter table if exists incident_response
    add constraint incident_response_report_fk foreign key (incident_id) references incident_report;

create table response
(
    id                   serial       not null
        constraint response_pk primary key,
    incident_response_id bigint       not null,
    data_point_value     varchar(120) not null,
    more_info            text
);

alter table if exists response
    add constraint response_incident_response_fk foreign key (incident_response_id) references incident_response;

create table other_person_involvement
(
    id          serial       not null
        constraint other_person_involvement_pk primary key,
    incident_id UUID         not null,
    person_name varchar(255) not null,
    person_type varchar(80)  not null
);

alter table if exists other_person_involvement
    add constraint other_person_involvement_report_fk foreign key (incident_id) references incident_report;

create table prisoner_involvement
(
    id                   serial      not null
        constraint prisoner_involvement_pk primary key,
    incident_id          UUID        not null,
    prisoner_number      varchar(7)  not null,
    prisoner_involvement varchar(80) not null
);

alter table if exists prisoner_involvement
    add constraint prisoner_involvement_report_fk foreign key (incident_id) references incident_report;

create table staff_involvement
(
    id             serial       not null
        constraint staff_involvement_pk primary key,
    incident_id    UUID         not null,
    staff_role     varchar(80)  not null,
    staff_username varchar(120) not null
);
alter table if exists staff_involvement
    add constraint staff_involvement_report_fk foreign key (incident_id) references incident_report;

create table status_history
(
    id          serial       not null
        constraint status_history_pk primary key,
    incident_id UUID         not null,
    status      varchar(30)  not null,
    set_on      timestamp    not null,
    set_by      varchar(120) not null
);

alter table if exists status_history
    add constraint status_history_report_fk foreign key (incident_id) references incident_report;


create table historical_incident_response
(
    id                          serial       not null
        constraint historical_incident_response_pk primary key,
    incident_id                 UUID         not null,
    data_point_key              varchar(120) not null,
    comment                     text,
    location_id                 bigint,
    other_person_involvement_id bigint,
    prisoner_involvement_id     bigint,
    evidence_id                 bigint,
    staff_involvement_id        bigint,
    recorded_on                 timestamp    not null,
    recorded_by                 varchar(120) not null
);

alter table if exists historical_incident_response
    add constraint historical_incident_response_report_fk foreign key (incident_id) references incident_report;

create table historical_response
(
    id                   serial       not null
        constraint historical_response_pk primary key,
    incident_response_id bigint       not null,
    data_point_value     varchar(120) not null,
    more_info            text
);

alter table if exists historical_response
    add constraint historical_response_incident_fk foreign key (incident_response_id) references historical_incident_response;


alter table if exists incident_response
    add constraint incident_response_evidence_fk foreign key (evidence_id) references evidence;
alter table if exists incident_response
    add constraint incident_response_location_fk foreign key (location_id) references incident_location;
alter table if exists incident_response
    add constraint incident_response_other_people_fk foreign key (other_person_involvement_id) references other_person_involvement;
alter table if exists incident_response
    add constraint incident_response_prisoner_fk foreign key (prisoner_involvement_id) references prisoner_involvement;
alter table if exists incident_response
    add constraint incident_response_staff_fk foreign key (staff_involvement_id) references staff_involvement;


alter table if exists historical_incident_response
    add constraint historical_incident_response_evidence_fk foreign key (evidence_id) references evidence;
alter table if exists historical_incident_response
    add constraint historical_incident_response_location_fk foreign key (location_id) references incident_location;
alter table if exists historical_incident_response
    add constraint historical_incident_response_other_people_fk foreign key (other_person_involvement_id) references other_person_involvement;
alter table if exists historical_incident_response
    add constraint historical_incident_response_prisoner_fk foreign key (prisoner_involvement_id) references prisoner_involvement;
alter table if exists historical_incident_response
    add constraint historical_incident_response_staff_fk foreign key (staff_involvement_id) references staff_involvement;