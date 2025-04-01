-- This is a COPY of constants/enumerations for use in reporting; Analytical Platform & DPR.
--
-- The application will continue to only use all values solely from internal constants
-- in `uk.gov.justice.digital.hmpps.incidentreporting.constants`.
--
-- NB:
--   - any changes to constants / enumeration classes REQUIRE new migrations!
--   - these tables should NOT be used in foreign key constaints! otherwise migrations would be overly-complicated

create table constant_type_family
(
  code        varchar(60) primary key,
  sequence    integer     not null,
  description varchar(60) not null
);

insert into constant_type_family(sequence, code, description)
values (0, 'ABSCOND', 'Abscond'),
       (1, 'ASSAULT', 'Assault'),
       (2, 'ATTEMPTED_ESCAPE_FROM_ESCORT', 'Attempted escape from escort'),
       (3, 'ATTEMPTED_ESCAPE_FROM_PRISON', 'Attempted escape from establishment'),
       (4, 'BARRICADE', 'Barricade'),
       (5, 'BOMB', 'Bomb explosion or threat'),
       (6, 'BREACH_OF_SECURITY', 'Breach or attempted breach of security'),
       (7, 'CLOSE_DOWN_SEARCH', 'Close down search'),
       (8, 'CONCERTED_INDISCIPLINE', 'Incident involving 2 or more prisioners acting together'),
       (9, 'DAMAGE', 'Deliberate damage to prison property'),
       (10, 'DEATH_OTHER', 'Death of other person'),
       (11, 'DEATH_PRISONER', 'Death of prisoner'),
       (12, 'DISORDER', 'Disorder'),
       (13, 'DRONE_SIGHTING', 'Drone sighting'),
       (14, 'DRUGS', 'Drugs'),
       (15, 'ESCAPE_FROM_ESCORT', 'Escape from escort'),
       (16, 'ESCAPE_FROM_PRISON', 'Escape from establishment'),
       (17, 'FIND', 'Find of illicit items'),
       (18, 'FIRE', 'Fire'),
       (19, 'FIREARM', 'Firearm, ammunition or chemical incapacitant'),
       (20, 'FOOD_REFUSAL', 'Food or liquid refusual'),
       (21, 'HOSTAGE', 'Hostage incident'),
       (22, 'INCIDENT_AT_HEIGHT', 'Incident at height'),
       (23, 'KEY_OR_LOCK', 'Key or lock compromise'),
       (24, 'MISCELLANEOUS', 'Miscellaneous'),
       (25, 'MOBILE_PHONE', 'Mobile phone'),
       (26, 'RADIO_COMPROMISE', 'Radio compromise'),
       (27, 'RELEASE_IN_ERROR', 'Release in error'),
       (28, 'SELF_HARM', 'Self harm'),
       (29, 'TEMPORARY_RELEASE_FAILURE', 'Temporary release failure'),
       (30, 'TOOL_LOSS', 'Tool or implement loss');

alter table constant_type
  add column family_code varchar(60);

update constant_type
set family_code='ABSCOND',
    description='Abscond'
where code = 'ABSCONDER';
update constant_type
set family_code='ASSAULT',
    description='Assault'
where code = 'ASSAULT';
update constant_type
set family_code='ATTEMPTED_ESCAPE_FROM_PRISON',
    description='Attempted escape from establishment'
where code = 'ATTEMPTED_ESCAPE_FROM_CUSTODY';
update constant_type
set family_code='ATTEMPTED_ESCAPE_FROM_ESCORT',
    description='Attempted escape from escort'
where code = 'ATTEMPTED_ESCAPE_FROM_ESCORT';
update constant_type
set family_code='BOMB',
    description='Bomb explosion or threat'
where code = 'BOMB_THREAT';
update constant_type
set family_code='BREACH_OF_SECURITY',
    description='Breach or attempted breach of security'
where code = 'BREACH_OF_SECURITY';
update constant_type
set family_code='DEATH_PRISONER',
    description='Death of prisoner'
where code = 'DEATH_IN_CUSTODY';
update constant_type
set family_code='DEATH_OTHER',
    description='Death of other person'
where code = 'DEATH_OTHER';
update constant_type
set family_code='DISORDER',
    description='Disorder'
where code = 'DISORDER';
update constant_type
set family_code='DRONE_SIGHTING',
    description='Drone sighting'
where code = 'DRONE_SIGHTING';
update constant_type
set family_code='ESCAPE_FROM_PRISON',
    description='Escape from establishment'
where code = 'ESCAPE_FROM_CUSTODY';
update constant_type
set family_code='ESCAPE_FROM_ESCORT',
    description='Escape from escort'
where code = 'ESCAPE_FROM_ESCORT';
update constant_type
set family_code='FIND',
    description='Find of illicit items'
where code = 'FINDS';
update constant_type
set family_code='FIRE',
    description='Fire'
where code = 'FIRE';
update constant_type
set family_code='FOOD_REFUSAL',
    description='Food or liquid refusual'
where code = 'FOOD_REFUSAL';
update constant_type
set family_code='CLOSE_DOWN_SEARCH',
    description='Close down search'
where code = 'FULL_CLOSE_DOWN_SEARCH';
update constant_type
set family_code='KEY_OR_LOCK',
    description='Key or lock compromise'
where code = 'KEY_LOCK_INCIDENT';
update constant_type
set family_code='MISCELLANEOUS',
    description='Miscellaneous'
where code = 'MISCELLANEOUS';
update constant_type
set family_code='RADIO_COMPROMISE',
    description='Radio compromise'
where code = 'RADIO_COMPROMISE';
update constant_type
set family_code='RELEASE_IN_ERROR',
    description='Release in error'
where code = 'RELEASED_IN_ERROR';
update constant_type
set family_code='SELF_HARM',
    description='Self harm'
where code = 'SELF_HARM';
update constant_type
set family_code='TEMPORARY_RELEASE_FAILURE',
    description='Temporary release failure'
where code = 'TEMPORARY_RELEASE_FAILURE';
update constant_type
set family_code='TOOL_LOSS',
    description='Tool or implement loss'
where code = 'TOOL_LOSS';
update constant_type
set family_code='DAMAGE',
    description='Deliberate damage to prison property'
where code = 'DAMAGE';
update constant_type
set family_code='ASSAULT',
    description='Assault'
where code = 'OLD_ASSAULT';
update constant_type
set family_code='ASSAULT',
    description='Assault'
where code = 'OLD_ASSAULT1';
update constant_type
set family_code='ASSAULT',
    description='Assault'
where code = 'OLD_ASSAULT2';
update constant_type
set family_code='ASSAULT',
    description='Assault'
where code = 'OLD_ASSAULT3';
update constant_type
set family_code='BARRICADE',
    description='Barricade'
where code = 'OLD_BARRICADE';
update constant_type
set family_code='CONCERTED_INDISCIPLINE',
    description='Incident involving 2 or more prisioners acting together'
where code = 'OLD_CONCERTED_INDISCIPLINE';
update constant_type
set family_code='DISORDER',
    description='Disorder'
where code = 'OLD_DISORDER';
update constant_type
set family_code='DRONE_SIGHTING',
    description='Drone sighting'
where code = 'OLD_DRONE_SIGHTING';
update constant_type
set family_code='DRONE_SIGHTING',
    description='Drone sighting'
where code = 'OLD_DRONE_SIGHTING1';
update constant_type
set family_code='DRUGS',
    description='Drugs'
where code = 'OLD_DRUGS';
update constant_type
set family_code='FIND',
    description='Find of illicit items'
where code = 'OLD_FINDS';
update constant_type
set family_code='FIND',
    description='Find of illicit items'
where code = 'OLD_FINDS1';
update constant_type
set family_code='FIND',
    description='Find of illicit items'
where code = 'OLD_FINDS2';
update constant_type
set family_code='FIND',
    description='Find of illicit items'
where code = 'OLD_FINDS3';
update constant_type
set family_code='FIND',
    description='Find of illicit items'
where code = 'OLD_FINDS4';
update constant_type
set family_code='FIREARM',
    description='Firearm, ammunition or chemical incapacitant'
where code = 'OLD_FIREARM_ETC';
update constant_type
set family_code='HOSTAGE',
    description='Hostage incident'
where code = 'OLD_HOSTAGE';
update constant_type
set family_code='KEY_OR_LOCK',
    description='Key or lock compromise'
where code = 'OLD_KEY_LOCK_INCIDENT';
update constant_type
set family_code='MOBILE_PHONE',
    description='Mobile phone'
where code = 'OLD_MOBILES';
update constant_type
set family_code='INCIDENT_AT_HEIGHT',
    description='Incident at height'
where code = 'OLD_ROOF_CLIMB';
update constant_type
set family_code='TEMPORARY_RELEASE_FAILURE',
    description='Temporary release failure'
where code = 'OLD_TEMPORARY_RELEASE_FAILURE';
update constant_type
set family_code='TEMPORARY_RELEASE_FAILURE',
    description='Temporary release failure'
where code = 'OLD_TEMPORARY_RELEASE_FAILURE1';
update constant_type
set family_code='TEMPORARY_RELEASE_FAILURE',
    description='Temporary release failure'
where code = 'OLD_TEMPORARY_RELEASE_FAILURE2';
