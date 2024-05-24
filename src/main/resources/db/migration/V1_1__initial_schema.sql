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
    created_at          timestamp                           not null,
    modified_by         varchar(120)                        not null,
    modified_at         timestamp default CURRENT_TIMESTAMP not null
);

create index event_event_date_and_time_idx on event (event_date_and_time);
create index event_created_at_idx on event (created_at);

create table report
(
    id                     uuid                                 not null
        constraint report_pk primary key,
    event_id               integer                              not null
        constraint report_event_fk references event (id) on delete restrict,
    incident_number        varchar(25)                          not null
        constraint incident_number unique,
    type                   varchar(60)                          not null,
    question_set_id        varchar(20),
    status                 varchar(60)                          not null,
    incident_date_and_time timestamp                            not null,
    prison_id              varchar(6)                           not null,
    reported_at            timestamp                            not null,
    assigned_to            varchar(120)                         not null,
    title                  varchar(255)                         not null,
    description            text                                 not null,
    reported_by            varchar(120)                         not null,
    created_at             timestamp                            not null,
    source                 varchar(5) default 'DPS'             not null,
    modified_by            varchar(120)                         not null,
    modified_at            timestamp  default CURRENT_TIMESTAMP not null
);

create index report_incident_date_and_time_idx on report (incident_date_and_time);
create index report_reported_at_idx on report (reported_at);
create index report_created_at_idx on report (created_at);

create index report_prison_id_idx on report (prison_id);
create index report_source_idx on report (source);
create index report_status_idx on report (status);
create index report_type_idx on report (type);

create table status_history
(
    id         serial
        constraint status_history_pk primary key,
    report_id  uuid                                not null
        constraint status_history_report_fk references report (id) on delete cascade,
    status     varchar(60)                         not null,
    changed_at timestamp default CURRENT_TIMESTAMP not null,
    changed_by varchar(120)                        not null
);

create index status_history_changed_at_idx on status_history (changed_at);

create table correction_request
(
    id                      serial
        constraint correction_request_pk primary key,
    report_id               uuid         not null
        constraint correction_request_report_fk references report (id) on delete cascade,
    correction_requested_by varchar(120) not null,
    reason                  varchar(60)  not null,
    description_of_change   text         not null,
    correction_requested_at timestamp    not null
);

create index correction_request_correction_requested_at_idx on correction_request (correction_requested_at);

create table prisoner_involvement
(
    id              serial
        constraint prisoner_involvement_pk primary key,
    report_id       uuid        not null
        constraint prisoner_involvement_report_fk references report (id) on delete cascade,
    prisoner_number varchar(7)  not null,
    prisoner_role   varchar(60) not null,
    outcome         varchar(60),
    comment         text
);

create table staff_involvement
(
    id             serial
        constraint staff_involvement_pk primary key,
    report_id      uuid         not null
        constraint staff_involvement_report_fk references report (id) on delete cascade,
    staff_role     varchar(60)  not null,
    staff_username varchar(120) not null,
    comment        text
);

create table evidence
(
    id          serial
        constraint evidence_pk primary key,
    report_id   uuid        not null
        constraint evidence_report_fk references report (id) on delete cascade,
    type        varchar(60) not null,
    description text        not null
);

create table location
(
    id          serial
        constraint location_pk primary key,
    report_id   uuid        not null
        constraint location_report_fk references report (id) on delete cascade,
    location_id varchar(60) not null,
    type        varchar(60) not null,
    description text        not null
);

create table question
(
    id                     serial
        constraint question_pk primary key,
    report_id              uuid              not null
        constraint question_report_fk references report (id) on delete cascade,
    sequence               integer default 0 not null,
    code                   varchar(60)       not null,
    question               text              not null,
    additional_information text
);

create index question_sequence_at_idx on question (sequence);

create table response
(
    id                     serial
        constraint response_pk primary key,
    question_id            integer                                not null
        constraint response_question_fk references question (id) on delete cascade,
    sequence               integer      default 0                 not null,
    response               text                                   not null,
    additional_information text,
    recorded_at            timestamp    default CURRENT_TIMESTAMP not null,
    recorded_by            varchar(120) default 'system'          not null
);

create index response_sequence_at_idx on response (sequence);

create table history
(
    id         serial
        constraint history_pk primary key,
    report_id  uuid         not null
        constraint history_report_fk references report (id) on delete cascade,
    type       varchar(60)  not null,
    changed_at timestamp    not null,
    changed_by varchar(120) not null
);

create index history_changed_at_idx on history (changed_at);

create table historical_question
(
    id                     serial
        constraint historical_question_pk primary key,
    history_id             integer           not null
        constraint historical_question_history_fk references history (id) on delete cascade,
    sequence               integer default 0 not null,
    code                   varchar(60)       not null,
    question               text              not null,
    additional_information text
);

create index historical_question_sequence_at_idx on historical_question (sequence);

create table historical_response
(
    id                     serial
        constraint historical_response_pk primary key,
    historical_question_id integer                                not null
        constraint historical_response_historical_question_fk references historical_question (id) on delete cascade,
    sequence               integer      default 0                 not null,
    response               text                                   not null,
    additional_information text,
    recorded_at            timestamp    default CURRENT_TIMESTAMP not null,
    recorded_by            varchar(120) default 'system'          not null
);

create index historical_response_sequence_at_idx on historical_response (sequence);
