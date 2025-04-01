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

-- add new type families
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

-- put types into families and rename codes to match
update constant_type
set family_code='ABSCOND',
    code='ABSCOND_1',
    description='Abscond'
where code = 'ABSCONDER';
update constant_type
set family_code='ASSAULT',
    code='ASSAULT_5',
    description='Assault'
where code = 'ASSAULT';
update constant_type
set family_code='ATTEMPTED_ESCAPE_FROM_PRISON',
    code='ATTEMPTED_ESCAPE_FROM_PRISON_1',
    description='Attempted escape from establishment'
where code = 'ATTEMPTED_ESCAPE_FROM_CUSTODY';
update constant_type
set family_code='ATTEMPTED_ESCAPE_FROM_ESCORT',
    code='ATTEMPTED_ESCAPE_FROM_ESCORT_1',
    description='Attempted escape from escort'
where code = 'ATTEMPTED_ESCAPE_FROM_ESCORT';
update constant_type
set family_code='BOMB',
    code='BOMB_1',
    description='Bomb explosion or threat'
where code = 'BOMB_THREAT';
update constant_type
set family_code='BREACH_OF_SECURITY',
    code='BREACH_OF_SECURITY_1',
    description='Breach or attempted breach of security'
where code = 'BREACH_OF_SECURITY';
update constant_type
set family_code='DEATH_PRISONER',
    code='DEATH_PRISONER_1',
    description='Death of prisoner'
where code = 'DEATH_IN_CUSTODY';
update constant_type
set family_code='DEATH_OTHER',
    code='DEATH_OTHER_1',
    description='Death of other person'
where code = 'DEATH_OTHER';
update constant_type
set family_code='DISORDER',
    code='DISORDER_2',
    description='Disorder'
where code = 'DISORDER';
update constant_type
set family_code='DRONE_SIGHTING',
    code='DRONE_SIGHTING_3',
    description='Drone sighting'
where code = 'DRONE_SIGHTING';
update constant_type
set family_code='ESCAPE_FROM_PRISON',
    code='ESCAPE_FROM_PRISON_1',
    description='Escape from establishment'
where code = 'ESCAPE_FROM_CUSTODY';
update constant_type
set family_code='ESCAPE_FROM_ESCORT',
    code='ESCAPE_FROM_ESCORT_1',
    description='Escape from escort'
where code = 'ESCAPE_FROM_ESCORT';
update constant_type
set family_code='FIND',
    code='FIND_6',
    description='Find of illicit items'
where code = 'FINDS';
update constant_type
set family_code='FIRE',
    code='FIRE_1',
    description='Fire'
where code = 'FIRE';
update constant_type
set family_code='FOOD_REFUSAL',
    code='FOOD_REFUSAL_1',
    description='Food or liquid refusual'
where code = 'FOOD_REFUSAL';
update constant_type
set family_code='CLOSE_DOWN_SEARCH',
    code='CLOSE_DOWN_SEARCH_1',
    description='Close down search'
where code = 'FULL_CLOSE_DOWN_SEARCH';
update constant_type
set family_code='KEY_OR_LOCK',
    code='KEY_OR_LOCK_2',
    description='Key or lock compromise'
where code = 'KEY_LOCK_INCIDENT';
update constant_type
set family_code='MISCELLANEOUS',
    code='MISCELLANEOUS_1',
    description='Miscellaneous'
where code = 'MISCELLANEOUS';
update constant_type
set family_code='RADIO_COMPROMISE',
    code='RADIO_COMPROMISE_1',
    description='Radio compromise'
where code = 'RADIO_COMPROMISE';
update constant_type
set family_code='RELEASE_IN_ERROR',
    code='RELEASE_IN_ERROR_1',
    description='Release in error'
where code = 'RELEASED_IN_ERROR';
update constant_type
set family_code='SELF_HARM',
    code='SELF_HARM_1',
    description='Self harm'
where code = 'SELF_HARM';
update constant_type
set family_code='TEMPORARY_RELEASE_FAILURE',
    code='TEMPORARY_RELEASE_FAILURE_4',
    description='Temporary release failure'
where code = 'TEMPORARY_RELEASE_FAILURE';
update constant_type
set family_code='TOOL_LOSS',
    code='TOOL_LOSS_1',
    description='Tool or implement loss'
where code = 'TOOL_LOSS';
update constant_type
set family_code='DAMAGE',
    code='DAMAGE_1',
    description='Deliberate damage to prison property'
where code = 'DAMAGE';
update constant_type
set family_code='ASSAULT',
    code='ASSAULT_1',
    description='Assault'
where code = 'OLD_ASSAULT';
update constant_type
set family_code='ASSAULT',
    code='ASSAULT_2',
    description='Assault'
where code = 'OLD_ASSAULT1';
update constant_type
set family_code='ASSAULT',
    code='ASSAULT_3',
    description='Assault'
where code = 'OLD_ASSAULT2';
update constant_type
set family_code='ASSAULT',
    code='ASSAULT_4',
    description='Assault'
where code = 'OLD_ASSAULT3';
update constant_type
set family_code='BARRICADE',
    code='BARRICADE_1',
    description='Barricade'
where code = 'OLD_BARRICADE';
update constant_type
set family_code='CONCERTED_INDISCIPLINE',
    code='CONCERTED_INDISCIPLINE_1',
    description='Incident involving 2 or more prisioners acting together'
where code = 'OLD_CONCERTED_INDISCIPLINE';
update constant_type
set family_code='DISORDER',
    code='DISORDER_1',
    description='Disorder'
where code = 'OLD_DISORDER';
update constant_type
set family_code='DRONE_SIGHTING',
    code='DRONE_SIGHTING_1',
    description='Drone sighting'
where code = 'OLD_DRONE_SIGHTING';
update constant_type
set family_code='DRONE_SIGHTING',
    code='DRONE_SIGHTING_2',
    description='Drone sighting'
where code = 'OLD_DRONE_SIGHTING1';
update constant_type
set family_code='DRUGS',
    code='DRUGS_1',
    description='Drugs'
where code = 'OLD_DRUGS';
update constant_type
set family_code='FIND',
    code='FIND_1',
    description='Find of illicit items'
where code = 'OLD_FINDS';
update constant_type
set family_code='FIND',
    code='FIND_2',
    description='Find of illicit items'
where code = 'OLD_FINDS1';
update constant_type
set family_code='FIND',
    code='FIND_3',
    description='Find of illicit items'
where code = 'OLD_FINDS2';
update constant_type
set family_code='FIND',
    code='FIND_4',
    description='Find of illicit items'
where code = 'OLD_FINDS3';
update constant_type
set family_code='FIND',
    code='FIND_5',
    description='Find of illicit items'
where code = 'OLD_FINDS4';
update constant_type
set family_code='FIREARM',
    code='FIREARM_1',
    description='Firearm, ammunition or chemical incapacitant'
where code = 'OLD_FIREARM_ETC';
update constant_type
set family_code='HOSTAGE',
    code='HOSTAGE_1',
    description='Hostage incident'
where code = 'OLD_HOSTAGE';
update constant_type
set family_code='KEY_OR_LOCK',
    code='KEY_OR_LOCK_1',
    description='Key or lock compromise'
where code = 'OLD_KEY_LOCK_INCIDENT';
update constant_type
set family_code='MOBILE_PHONE',
    code='MOBILE_PHONE_1',
    description='Mobile phone'
where code = 'OLD_MOBILES';
update constant_type
set family_code='INCIDENT_AT_HEIGHT',
    code='INCIDENT_AT_HEIGHT_1',
    description='Incident at height'
where code = 'OLD_ROOF_CLIMB';
update constant_type
set family_code='TEMPORARY_RELEASE_FAILURE',
    code='TEMPORARY_RELEASE_FAILURE_1',
    description='Temporary release failure'
where code = 'OLD_TEMPORARY_RELEASE_FAILURE';
update constant_type
set family_code='TEMPORARY_RELEASE_FAILURE',
    code='TEMPORARY_RELEASE_FAILURE_2',
    description='Temporary release failure'
where code = 'OLD_TEMPORARY_RELEASE_FAILURE1';
update constant_type
set family_code='TEMPORARY_RELEASE_FAILURE',
    code='TEMPORARY_RELEASE_FAILURE_3',
    description='Temporary release failure'
where code = 'OLD_TEMPORARY_RELEASE_FAILURE2';

update constant_status
set description='Information amended'
where code = 'INFORMATION_AMENDED';
