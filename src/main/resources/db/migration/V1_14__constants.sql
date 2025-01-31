-- This is a COPY of constants/enumerations for use in reporting; Analytical Platform & DPR.
--
-- The application will continue to only use all values solely from internal constants
-- in `uk.gov.justice.digital.hmpps.incidentreporting.constants`.
--
-- NB:
--   - any changes to constants / enumeration classes REQUIRE new migrations!
--   - these tables should NOT be used in foreign key constaints! otherwise migrations would be overly-complicated
--   - NOMIS codes are not included as DPR reports are being recreated and there’s no need to preserve a link to data that will be removed

create table constant_prisoner_outcome
(
  code        varchar(60) primary key,
  sequence    integer     not null,
  description varchar(60) not null
);

create table constant_prisoner_role
(
  code        varchar(60) primary key,
  sequence    integer     not null,
  description varchar(60) not null
);

create table constant_staff_role
(
  code        varchar(60) primary key,
  sequence    integer     not null,
  description varchar(60) not null
);

create table constant_status
(
  code        varchar(60) primary key,
  sequence    integer     not null,
  description varchar(60) not null
);

create table constant_type
(
  code        varchar(60) primary key,
  sequence    integer     not null,
  description varchar(60) not null,
  active      boolean default true
);

insert into constant_prisoner_outcome(sequence, code, description)
values (0, 'ACCT', 'ACCT'),
       (1, 'CHARGED_BY_POLICE', 'Charged by Police'),
       (2, 'CONVICTED', 'Convicted'),
       (3, 'CORONER_INFORMED', 'Coroner informed'),
       (4, 'DEATH', 'Death'),
       (5, 'FURTHER_CHARGES', 'Further charges'),
       (6, 'LOCAL_INVESTIGATION', 'Investigation (local)'),
       (7, 'NEXT_OF_KIN_INFORMED', 'Next of kin informed'),
       (8, 'PLACED_ON_REPORT', 'Placed on report'),
       (9, 'POLICE_INVESTIGATION', 'Investigation (Police)'),
       (10, 'REMAND', 'Remand'),
       (11, 'SEEN_DUTY_GOV', 'Seen by Duty Governor'),
       (12, 'SEEN_HEALTHCARE', 'Seen by Healthcare'),
       (13, 'SEEN_IMB', 'Seen by IMB'),
       (14, 'SEEN_OUTSIDE_HOSP', 'Seen by outside hospital'),
       (15, 'TRANSFER', 'Transfer'),
       (16, 'TRIAL', 'Trial');

insert into constant_prisoner_role(sequence, code, description)
values (0, 'ABSCONDER', 'Absconder'),
       (1, 'ACTIVE_INVOLVEMENT', 'Active involvement'),
       (2, 'ASSAILANT', 'Assailant'),
       (3, 'ASSISTED_STAFF', 'Assisted staff'),
       (4, 'DECEASED', 'Deceased'),
       (5, 'ESCAPE', 'Escapee'),
       (6, 'FIGHTER', 'Fighter'),
       (7, 'HOSTAGE', 'Hostage'),
       (8, 'IMPEDED_STAFF', 'Impeded staff'),
       (9, 'IN_POSSESSION', 'In possession'),
       (10, 'INTENDED_RECIPIENT', 'Intended recipient'),
       (11, 'LICENSE_FAILURE', 'License failure'),
       (12, 'PERPETRATOR', 'Perpetrator'),
       (13, 'PRESENT_AT_SCENE', 'Present at scene'),
       (14, 'SUSPECTED_ASSAILANT', 'Suspected assailant'),
       (15, 'SUSPECTED_INVOLVED', 'Suspected involved'),
       (16, 'TEMPORARY_RELEASE_FAILURE', 'Temporary release failure'),
       (17, 'VICTIM', 'Victim');

insert into constant_staff_role(sequence, code, description)
values (0, 'ACTIVELY_INVOLVED', 'Actively involved'),
       (1, 'AUTHORISING_OFFICER', 'Authorising officer'),
       (2, 'CR_HEAD', 'Control and restraint - head'),
       (3, 'CR_LEFT_ARM', 'Control and restraint - left arm'),
       (4, 'CR_LEGS', 'Control and restraint - legs'),
       (5, 'CR_RIGHT_ARM', 'Control and restraint - right arm'),
       (6, 'CR_SUPERVISOR', 'Control and restraint - supervisor'),
       (7, 'DECEASED', 'Deceased'),
       (8, 'FIRST_ON_SCENE', 'First on scene'),
       (9, 'HEALTHCARE', 'Healthcare'),
       (10, 'HOSTAGE', 'Hostage'),
       (11, 'IN_POSSESSION', 'In possession'),
       (12, 'NEGOTIATOR', 'Negotiator'),
       (13, 'PRESENT_AT_SCENE', 'Present at scene'),
       (14, 'SUSPECTED_INVOLVEMENT', 'Suspected involvement'),
       (15, 'VICTIM', 'Victim'),
       (16, 'WITNESS', 'Witness');

insert into constant_status(sequence, code, description)
values (0, 'DRAFT', 'Draft'),
       (1, 'AWAITING_ANALYSIS', 'Awaiting analysis'),
       (2, 'IN_ANALYSIS', 'In analysis'),
       (3, 'INFORMATION_REQUIRED', 'Information required'),
       (4, 'INFORMATION_AMENDED', 'Information amened'),
       (5, 'CLOSED', 'Closed'),
       (6, 'POST_INCIDENT_UPDATE', 'Post-incident update'),
       (7, 'INCIDENT_UPDATED', 'Incident updated'),
       (8, 'DUPLICATE', 'Duplicate');

insert into constant_type(sequence, code, description, active)
values (0, 'ABSCONDER', 'Absconder', true),
       (1, 'ASSAULT', 'Assault', true),
       (2, 'ATTEMPTED_ESCAPE_FROM_CUSTODY', 'Attempted escape from custody', true),
       (3, 'ATTEMPTED_ESCAPE_FROM_ESCORT', 'Attempted escape from escort', true),
       (4, 'BOMB_THREAT', 'Bomb threat', true),
       (5, 'BREACH_OF_SECURITY', 'Breach of security', true),
       (6, 'DEATH_IN_CUSTODY', 'Death in custody', true),
       (7, 'DEATH_OTHER', 'Death (other)', true),
       (8, 'DISORDER', 'Disorder', true),
       (9, 'DRONE_SIGHTING', 'Drone sighting', true),
       (10, 'ESCAPE_FROM_CUSTODY', 'Escape from custody', true),
       (11, 'ESCAPE_FROM_ESCORT', 'Escape from escort', true),
       (12, 'FINDS', 'Finds', true),
       (13, 'FIRE', 'Fire', true),
       (14, 'FOOD_REFUSAL', 'Food refusal', true),
       (15, 'FULL_CLOSE_DOWN_SEARCH', 'Full close down search', true),
       (16, 'KEY_LOCK_INCIDENT', 'Key lock incident', true),
       (17, 'MISCELLANEOUS', 'Miscellaneous', true),
       (18, 'RADIO_COMPROMISE', 'Radio compromise', true),
       (19, 'RELEASED_IN_ERROR', 'Released in error', true),
       (20, 'SELF_HARM', 'Self harm', true),
       (21, 'TEMPORARY_RELEASE_FAILURE', 'Temporary release failure', true),
       (22, 'TOOL_LOSS', 'Tool loss', true),
       (23, 'DAMAGE', 'Damage', false),
       (24, 'OLD_ASSAULT', 'Assault', false),
       (25, 'OLD_ASSAULT1', 'Assault (from April 2017)', false),
       (26, 'OLD_ASSAULT2', 'Assault (from April 2017)', false),
       (27, 'OLD_ASSAULT3', 'Assault (from April 2017)', false),
       (28, 'OLD_BARRICADE', 'Barricade/prevention of access', false),
       (29, 'OLD_CONCERTED_INDISCIPLINE', 'Concerted indiscipline', false),
       (30, 'OLD_DISORDER', 'Disorder', false),
       (31, 'OLD_DRONE_SIGHTING', 'Drone sighting', false),
       (32, 'OLD_DRONE_SIGHTING1', 'Drone sighting (from 2017)', false),
       (33, 'OLD_DRUGS', 'Drugs', false),
       (34, 'OLD_FINDS', 'Finds', false),
       (35, 'OLD_FINDS1', 'Finds (from August 2015)', false),
       (36, 'OLD_FINDS2', 'Finds (from September 2015)', false),
       (37, 'OLD_FINDS3', 'Finds (from March 2022)', false),
       (38, 'OLD_FINDS4', 'Finds (from September 2016)', false),
       (39, 'OLD_FIREARM_ETC', 'Firearm/ammunition/chemical incapacitant', false),
       (40, 'OLD_HOSTAGE', 'Hostage', false),
       (41, 'OLD_KEY_LOCK_INCIDENT', 'Key lock incident', false),
       (42, 'OLD_MOBILES', 'Mobile phones', false),
       (43, 'OLD_ROOF_CLIMB', 'Incident at height', false),
       (44, 'OLD_TEMPORARY_RELEASE_FAILURE', 'Temporary release failure', false),
       (45, 'OLD_TEMPORARY_RELEASE_FAILURE1', 'Temporary release failure (from July 2015)', false),
       (46, 'OLD_TEMPORARY_RELEASE_FAILURE2', 'Temporary release failure (from April 2016)', false);
