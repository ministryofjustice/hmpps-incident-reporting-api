ALTER TABLE correction_request ADD COLUMN user_action varchar(60);
ALTER TABLE correction_request ADD COLUMN original_report_reference varchar(25);
ALTER TABLE correction_request ADD COLUMN user_type varchar(60);

-- This is a COPY of constants/enumerations for use in reporting; Analytical Platform & DPR.
--
-- The application will continue to only use all values solely from internal constants
-- in `uk.gov.justice.digital.hmpps.incidentreporting.constants`.
--
-- NB:
--   - any changes to constants / enumeration classes REQUIRE new migrations!
--   - these tables should NOT be used in foreign key constraints! otherwise migrations would be overly-complicated

create table constant_user_action
(
  code        varchar(60) primary key,
  sequence    integer     not null,
  description varchar(60) not null
);

create table constant_user_type
(
  code        varchar(60) primary key,
  sequence    integer     not null,
  description varchar(60) not null
);

insert into constant_user_action(sequence, code, description)
values (0, 'REQUEST_DUPLICATE', 'Request to mark duplicate'),
       (1, 'REQUEST_NOT_REPORTABLE', 'Request to mark not reportable'),
       (2, 'REQUEST_CORRECTION', 'Request correction'),
       (3, 'RECALL', 'Recall'),
       (4, 'CLOSE', 'Close'),
       (5, 'MARK_DUPLICATE', 'Mark as duplicate'),
       (6, 'MARK_NOT_REPORTABLE', 'Mark as not reportable'),
       (7, 'HOLD', 'Put on hold');

insert into constant_user_type(sequence, code, description)
values (0, 'REPORTING_OFFICER', 'Reporting officer'),
       (1, 'DATA_WARDEN', 'Data warden');
