create sequence event_sequence
    start with 1000000;

create sequence report_sequence
    start with 1000000;

create table event
(
    id                  serial
        constraint event_pk primary key,
    event_id            varchar(25)                         not null
        constraint event_id unique,
    prison_id           varchar(6)                          not null,
    event_date_and_time timestamp                           not null,
    title               varchar(255)                        not null,
    description         text                                not null,
    created_date        timestamp                           not null,
    last_modified_by    varchar(120)                        not null,
    last_modified_date  timestamp default CURRENT_TIMESTAMP not null
);

create table report
(
    id                     uuid       default gen_random_uuid()        not null
        constraint report_pk primary key,
    event_id               bigint                                      not null
        constraint report_event_fk references event (id) on delete restrict,
    incident_number        varchar(25)                                 not null
        constraint incident_number unique,
    type                   varchar(255)                                not null,
    question_set_id        varchar(20),
    status                 varchar(255)                                not null,
    incident_date_and_time timestamp                                   not null,
    prison_id              varchar(6)                                  not null,
    reported_date          timestamp                                   not null,
    assigned_to            varchar(120)                                not null,
    title                  varchar(255)                                not null,
    description            text                                        not null,
    reported_by            varchar(120)                                not null,
    created_date           timestamp                                   not null,
    source                 varchar(5) default 'DPS'::character varying not null,
    last_modified_by       varchar(120)                                not null,
    last_modified_date     timestamp  default CURRENT_TIMESTAMP        not null
);

create table status_history
(
    id        serial
        constraint status_history_pk primary key,
    report_id uuid                                not null
        constraint status_history_report_fk references report (id) on delete cascade,
    status    varchar(30)                         not null,
    set_on    timestamp default CURRENT_TIMESTAMP not null,
    set_by    varchar(120)                        not null
);

create table correction_request
(
    id                      serial
        constraint correction_request_pk primary key,
    report_id               uuid         not null
        constraint correction_request_report_fk references report (id) on delete cascade,
    correction_requested_by varchar(120) not null,
    description_of_change   text         not null,
    reason                  varchar(80)  not null,
    correction_requested_at timestamp    not null
);

create table prisoner_involvement
(
    id                   serial
        constraint prisoner_involvement_pk primary key,
    report_id            uuid        not null
        constraint prisoner_involvement_report_fk references report (id) on delete cascade,
    prisoner_number      varchar(7)  not null,
    prisoner_involvement varchar(80) not null,
    comment              text,
    outcome              varchar(30)
);

create table staff_involvement
(
    id             serial
        constraint staff_involvement_pk primary key,
    report_id      uuid         not null
        constraint staff_involvement_report_fk references report (id) on delete cascade,
    staff_role     varchar(80)  not null,
    staff_username varchar(120) not null,
    comment        text
);

create table evidence
(
    id          serial
        constraint evidence_pk primary key,
    report_id   uuid        not null
        constraint evidence_report_fk references report (id) on delete cascade,
    type        varchar(80) not null,
    description text        not null
);

create table location
(
    id          serial
        constraint location_pk primary key,
    report_id   uuid         not null
        constraint location_report_fk references report (id) on delete cascade,
    location_id varchar(210) not null,
    type        varchar(80)  not null,
    description text         not null
);

create table question
(
    id                      serial
        constraint question_pk primary key,
    report_id               uuid              not null
        constraint question_report_fk references report (id) on delete cascade,
    sequence                integer default 0 not null,
    code                    varchar(120),
    question                text,
    additional_information  text,
    location_id             bigint
        constraint question_location_fk references location (id) on delete cascade,
    prisoner_involvement_id bigint
        constraint question_prisoner_involvement_fk references prisoner_involvement (id) on delete cascade,
    evidence_id             bigint
        constraint question_evidence_fk references evidence (id) on delete cascade,
    staff_involvement_id    bigint
        constraint question_staff_involvement_fk references staff_involvement (id) on delete cascade
);

create table response
(
    id                     serial
        constraint response_pk primary key,
    question_id            bigint                                           not null
        constraint response_question_fk references question (id) on delete cascade,
    sequence               integer      default 0                           not null,
    response               text                                             not null,
    additional_information text,
    recorded_on            timestamp    default CURRENT_TIMESTAMP           not null,
    recorded_by            varchar(120) default 'system'::character varying not null
);

create table history
(
    id                    serial
        constraint history_pk primary key,
    report_id             uuid         not null
        constraint history_report_fk references report (id) on delete cascade,
    type                  varchar(255) not null,
    change_date           timestamp    not null,
    change_staff_username varchar(120) not null
);

create table historical_question
(
    id                      serial
        constraint historical_question_pk primary key,
    history_id              bigint            not null
        constraint historical_question_history_fk references history (id) on delete cascade,
    sequence                integer default 0 not null,
    code                    varchar(120)      not null,
    question                text,
    additional_information  text,
    location_id             bigint
        constraint historical_question_location_fk references location (id) on delete cascade,
    prisoner_involvement_id bigint
        constraint historical_question_prisoner_involvement_fk references prisoner_involvement (id) on delete cascade,
    evidence_id             bigint
        constraint historical_question_evidence_fk references evidence (id) on delete cascade,
    staff_involvement_id    bigint
        constraint historical_question_staff_involvement_fk references staff_involvement (id) on delete cascade
);

create table historical_response
(
    id                     serial
        constraint historical_response_pk primary key,
    historical_question_id bigint                                           not null
        constraint historical_response_historical_question_fk references historical_question (id) on delete cascade,
    sequence               integer      default 0                           not null,
    response               text                                             not null,
    additional_information text,
    recorded_on            timestamp    default CURRENT_TIMESTAMP           not null,
    recorded_by            varchar(120) default 'system'::character varying not null
);
