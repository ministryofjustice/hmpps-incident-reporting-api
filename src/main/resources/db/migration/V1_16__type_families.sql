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
       (1, 'ATTEMPTED_ESCAPE_FROM_PRISON', 'Attempted escape from establishment'),
       (3, 'ATTEMPTED_ESCAPE_FROM_ESCORT', 'Attempted escape from escort'),
       (4, 'BARRICADE', 'Barricade'),
       (5, 'BOMB', 'Bomb explosion or threat'),
       (6, 'BREACH_OF_SECURITY', 'Breach or attempted breach of security'),
       (7, 'CLOSE_DOWN_SEARCH', 'Close down search'),
       (8, 'CONCERTED_INDISCIPLINE', 'Incident involving 2 or more prisioners acting together'),
       (9, 'DAMAGE', 'Deliberate damage to prison property'),
       (10, 'DEATH_PRISONER', 'Death of prisoner'),
       (11, 'DEATH_OTHER', 'Death of other person'),
       (12, 'DISORDER', 'Disorder'),
       (13, 'DRONE_SIGHTING', 'Drone sighting'),
       (14, 'DRUGS', 'Drugs'),
       (15, 'ESCAPE_FROM_PRISON', 'Escape from establishment'),
       (16, 'ESCAPE_FROM_ESCORT', 'Escape from escort'),
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
    sequence=0,
    description='Abscond'
where code = 'ABSCONDER';
update constant_type
set family_code='ASSAULT',
    code='ASSAULT_5',
    sequence=5,
    description='Assault'
where code = 'ASSAULT';
update constant_type
set family_code='ATTEMPTED_ESCAPE_FROM_PRISON',
    code='ATTEMPTED_ESCAPE_FROM_PRISON_1',
    sequence=6,
    description='Attempted escape from establishment'
where code = 'ATTEMPTED_ESCAPE_FROM_CUSTODY';
update constant_type
set family_code='ATTEMPTED_ESCAPE_FROM_ESCORT',
    code='ATTEMPTED_ESCAPE_FROM_ESCORT_1',
    sequence=7,
    description='Attempted escape from escort'
where code = 'ATTEMPTED_ESCAPE_FROM_ESCORT';
update constant_type
set family_code='BOMB',
    code='BOMB_1',
    sequence=9,
    description='Bomb explosion or threat'
where code = 'BOMB_THREAT';
update constant_type
set family_code='BREACH_OF_SECURITY',
    code='BREACH_OF_SECURITY_1',
    sequence=10,
    description='Breach or attempted breach of security'
where code = 'BREACH_OF_SECURITY';
update constant_type
set family_code='DEATH_PRISONER',
    code='DEATH_PRISONER_1',
    sequence=14,
    description='Death of prisoner'
where code = 'DEATH_IN_CUSTODY';
update constant_type
set family_code='DEATH_OTHER',
    code='DEATH_OTHER_1',
    sequence=15,
    description='Death of other person'
where code = 'DEATH_OTHER';
update constant_type
set family_code='DISORDER',
    code='DISORDER_2',
    sequence=17,
    description='Disorder'
where code = 'DISORDER';
update constant_type
set family_code='DRONE_SIGHTING',
    code='DRONE_SIGHTING_3',
    sequence=20,
    description='Drone sighting'
where code = 'DRONE_SIGHTING';
update constant_type
set family_code='ESCAPE_FROM_PRISON',
    code='ESCAPE_FROM_PRISON_1',
    sequence=22,
    description='Escape from establishment'
where code = 'ESCAPE_FROM_CUSTODY';
update constant_type
set family_code='ESCAPE_FROM_ESCORT',
    code='ESCAPE_FROM_ESCORT_1',
    sequence=23,
    description='Escape from escort'
where code = 'ESCAPE_FROM_ESCORT';
update constant_type
set family_code='FIND',
    code='FIND_6',
    sequence=29,
    description='Find of illicit items'
where code = 'FINDS';
update constant_type
set family_code='FIRE',
    code='FIRE_1',
    sequence=30,
    description='Fire'
where code = 'FIRE';
update constant_type
set family_code='FOOD_REFUSAL',
    code='FOOD_REFUSAL_1',
    sequence=32,
    description='Food or liquid refusual'
where code = 'FOOD_REFUSAL';
update constant_type
set family_code='CLOSE_DOWN_SEARCH',
    code='CLOSE_DOWN_SEARCH_1',
    sequence=11,
    description='Close down search'
where code = 'FULL_CLOSE_DOWN_SEARCH';
update constant_type
set family_code='KEY_OR_LOCK',
    code='KEY_OR_LOCK_2',
    sequence=36,
    description='Key or lock compromise'
where code = 'KEY_LOCK_INCIDENT';
update constant_type
set family_code='MISCELLANEOUS',
    code='MISCELLANEOUS_1',
    sequence=37,
    description='Miscellaneous'
where code = 'MISCELLANEOUS';
update constant_type
set family_code='RADIO_COMPROMISE',
    code='RADIO_COMPROMISE_1',
    sequence=39,
    description='Radio compromise'
where code = 'RADIO_COMPROMISE';
update constant_type
set family_code='RELEASE_IN_ERROR',
    code='RELEASE_IN_ERROR_1',
    sequence=40,
    description='Release in error'
where code = 'RELEASED_IN_ERROR';
update constant_type
set family_code='SELF_HARM',
    code='SELF_HARM_1',
    sequence=41,
    description='Self harm'
where code = 'SELF_HARM';
update constant_type
set family_code='TEMPORARY_RELEASE_FAILURE',
    code='TEMPORARY_RELEASE_FAILURE_4',
    sequence=45,
    description='Temporary release failure'
where code = 'TEMPORARY_RELEASE_FAILURE';
update constant_type
set family_code='TOOL_LOSS',
    code='TOOL_LOSS_1',
    sequence=46,
    description='Tool or implement loss'
where code = 'TOOL_LOSS';
update constant_type
set family_code='DAMAGE',
    code='DAMAGE_1',
    sequence=13,
    description='Deliberate damage to prison property'
where code = 'DAMAGE';
update constant_type
set family_code='ASSAULT',
    code='ASSAULT_1',
    sequence=1,
    description='Assault'
where code = 'OLD_ASSAULT';
update constant_type
set family_code='ASSAULT',
    code='ASSAULT_2',
    sequence=2,
    description='Assault'
where code = 'OLD_ASSAULT1';
update constant_type
set family_code='ASSAULT',
    code='ASSAULT_3',
    sequence=3,
    description='Assault'
where code = 'OLD_ASSAULT2';
update constant_type
set family_code='ASSAULT',
    code='ASSAULT_4',
    sequence=4,
    description='Assault'
where code = 'OLD_ASSAULT3';
update constant_type
set family_code='BARRICADE',
    code='BARRICADE_1',
    sequence=8,
    description='Barricade'
where code = 'OLD_BARRICADE';
update constant_type
set family_code='CONCERTED_INDISCIPLINE',
    code='CONCERTED_INDISCIPLINE_1',
    sequence=12,
    description='Incident involving 2 or more prisioners acting together'
where code = 'OLD_CONCERTED_INDISCIPLINE';
update constant_type
set family_code='DISORDER',
    code='DISORDER_1',
    sequence=16,
    description='Disorder'
where code = 'OLD_DISORDER';
update constant_type
set family_code='DRONE_SIGHTING',
    code='DRONE_SIGHTING_1',
    sequence=18,
    description='Drone sighting'
where code = 'OLD_DRONE_SIGHTING';
update constant_type
set family_code='DRONE_SIGHTING',
    code='DRONE_SIGHTING_2',
    sequence=19,
    description='Drone sighting'
where code = 'OLD_DRONE_SIGHTING1';
update constant_type
set family_code='DRUGS',
    code='DRUGS_1',
    sequence=21,
    description='Drugs'
where code = 'OLD_DRUGS';
update constant_type
set family_code='FIND',
    code='FIND_1',
    sequence=24,
    description='Find of illicit items'
where code = 'OLD_FINDS';
update constant_type
set family_code='FIND',
    code='FIND_2',
    sequence=25,
    description='Find of illicit items'
where code = 'OLD_FINDS1';
update constant_type
set family_code='FIND',
    code='FIND_3',
    sequence=26,
    description='Find of illicit items'
where code = 'OLD_FINDS2';
update constant_type
set family_code='FIND',
    code='FIND_4',
    sequence=27,
    description='Find of illicit items'
where code = 'OLD_FINDS3';
update constant_type
set family_code='FIND',
    code='FIND_5',
    sequence=28,
    description='Find of illicit items'
where code = 'OLD_FINDS4';
update constant_type
set family_code='FIREARM',
    code='FIREARM_1',
    sequence=31,
    description='Firearm, ammunition or chemical incapacitant'
where code = 'OLD_FIREARM_ETC';
update constant_type
set family_code='HOSTAGE',
    code='HOSTAGE_1',
    sequence=33,
    description='Hostage incident'
where code = 'OLD_HOSTAGE';
update constant_type
set family_code='KEY_OR_LOCK',
    code='KEY_OR_LOCK_1',
    sequence=35,
    description='Key or lock compromise'
where code = 'OLD_KEY_LOCK_INCIDENT';
update constant_type
set family_code='MOBILE_PHONE',
    code='MOBILE_PHONE_1',
    sequence=38,
    description='Mobile phone'
where code = 'OLD_MOBILES';
update constant_type
set family_code='INCIDENT_AT_HEIGHT',
    code='INCIDENT_AT_HEIGHT_1',
    sequence=34,
    description='Incident at height'
where code = 'OLD_ROOF_CLIMB';
update constant_type
set family_code='TEMPORARY_RELEASE_FAILURE',
    code='TEMPORARY_RELEASE_FAILURE_1',
    sequence=42,
    description='Temporary release failure'
where code = 'OLD_TEMPORARY_RELEASE_FAILURE';
update constant_type
set family_code='TEMPORARY_RELEASE_FAILURE',
    code='TEMPORARY_RELEASE_FAILURE_2',
    sequence=43,
    description='Temporary release failure'
where code = 'OLD_TEMPORARY_RELEASE_FAILURE1';
update constant_type
set family_code='TEMPORARY_RELEASE_FAILURE',
    code='TEMPORARY_RELEASE_FAILURE_3',
    sequence=44,
    description='Temporary release failure'
where code = 'OLD_TEMPORARY_RELEASE_FAILURE2';

-- update all reports and histories with new codes
update report
set type='ABSCOND_1'
where type = 'ABSCONDER';
update report
set type='ASSAULT_5'
where type = 'ASSAULT';
update report
set type='ATTEMPTED_ESCAPE_FROM_PRISON_1'
where type = 'ATTEMPTED_ESCAPE_FROM_CUSTODY';
update report
set type='ATTEMPTED_ESCAPE_FROM_ESCORT_1'
where type = 'ATTEMPTED_ESCAPE_FROM_ESCORT';
update report
set type='BOMB_1'
where type = 'BOMB_THREAT';
update report
set type='BREACH_OF_SECURITY_1'
where type = 'BREACH_OF_SECURITY';
update report
set type='DEATH_PRISONER_1'
where type = 'DEATH_IN_CUSTODY';
update report
set type='DEATH_OTHER_1'
where type = 'DEATH_OTHER';
update report
set type='DISORDER_2'
where type = 'DISORDER';
update report
set type='DRONE_SIGHTING_3'
where type = 'DRONE_SIGHTING';
update report
set type='ESCAPE_FROM_PRISON_1'
where type = 'ESCAPE_FROM_CUSTODY';
update report
set type='ESCAPE_FROM_ESCORT_1'
where type = 'ESCAPE_FROM_ESCORT';
update report
set type='FIND_6'
where type = 'FINDS';
update report
set type='FIRE_1'
where type = 'FIRE';
update report
set type='FOOD_REFUSAL_1'
where type = 'FOOD_REFUSAL';
update report
set type='CLOSE_DOWN_SEARCH_1'
where type = 'FULL_CLOSE_DOWN_SEARCH';
update report
set type='KEY_OR_LOCK_2'
where type = 'KEY_LOCK_INCIDENT';
update report
set type='MISCELLANEOUS_1'
where type = 'MISCELLANEOUS';
update report
set type='RADIO_COMPROMISE_1'
where type = 'RADIO_COMPROMISE';
update report
set type='RELEASE_IN_ERROR_1'
where type = 'RELEASED_IN_ERROR';
update report
set type='SELF_HARM_1'
where type = 'SELF_HARM';
update report
set type='TEMPORARY_RELEASE_FAILURE_4'
where type = 'TEMPORARY_RELEASE_FAILURE';
update report
set type='TOOL_LOSS_1'
where type = 'TOOL_LOSS';
update report
set type='DAMAGE_1'
where type = 'DAMAGE';
update report
set type='ASSAULT_1'
where type = 'OLD_ASSAULT';
update report
set type='ASSAULT_2'
where type = 'OLD_ASSAULT1';
update report
set type='ASSAULT_3'
where type = 'OLD_ASSAULT2';
update report
set type='ASSAULT_4'
where type = 'OLD_ASSAULT3';
update report
set type='BARRICADE_1'
where type = 'OLD_BARRICADE';
update report
set type='CONCERTED_INDISCIPLINE_1'
where type = 'OLD_CONCERTED_INDISCIPLINE';
update report
set type='DISORDER_1'
where type = 'OLD_DISORDER';
update report
set type='DRONE_SIGHTING_1'
where type = 'OLD_DRONE_SIGHTING';
update report
set type='DRONE_SIGHTING_2'
where type = 'OLD_DRONE_SIGHTING1';
update report
set type='DRUGS_1'
where type = 'OLD_DRUGS';
update report
set type='FIND_1'
where type = 'OLD_FINDS';
update report
set type='FIND_2'
where type = 'OLD_FINDS1';
update report
set type='FIND_3'
where type = 'OLD_FINDS2';
update report
set type='FIND_4'
where type = 'OLD_FINDS3';
update report
set type='FIND_5'
where type = 'OLD_FINDS4';
update report
set type='FIREARM_1'
where type = 'OLD_FIREARM_ETC';
update report
set type='HOSTAGE_1'
where type = 'OLD_HOSTAGE';
update report
set type='KEY_OR_LOCK_1'
where type = 'OLD_KEY_LOCK_INCIDENT';
update report
set type='MOBILE_PHONE_1'
where type = 'OLD_MOBILES';
update report
set type='INCIDENT_AT_HEIGHT_1'
where type = 'OLD_ROOF_CLIMB';
update report
set type='TEMPORARY_RELEASE_FAILURE_1'
where type = 'OLD_TEMPORARY_RELEASE_FAILURE';
update report
set type='TEMPORARY_RELEASE_FAILURE_2'
where type = 'OLD_TEMPORARY_RELEASE_FAILURE1';
update report
set type='TEMPORARY_RELEASE_FAILURE_3'
where type = 'OLD_TEMPORARY_RELEASE_FAILURE2';
update history
set type='ABSCOND_1'
where type = 'ABSCONDER';
update history
set type='ASSAULT_5'
where type = 'ASSAULT';
update history
set type='ATTEMPTED_ESCAPE_FROM_PRISON_1'
where type = 'ATTEMPTED_ESCAPE_FROM_CUSTODY';
update history
set type='ATTEMPTED_ESCAPE_FROM_ESCORT_1'
where type = 'ATTEMPTED_ESCAPE_FROM_ESCORT';
update history
set type='BOMB_1'
where type = 'BOMB_THREAT';
update history
set type='BREACH_OF_SECURITY_1'
where type = 'BREACH_OF_SECURITY';
update history
set type='DEATH_PRISONER_1'
where type = 'DEATH_IN_CUSTODY';
update history
set type='DEATH_OTHER_1'
where type = 'DEATH_OTHER';
update history
set type='DISORDER_2'
where type = 'DISORDER';
update history
set type='DRONE_SIGHTING_3'
where type = 'DRONE_SIGHTING';
update history
set type='ESCAPE_FROM_PRISON_1'
where type = 'ESCAPE_FROM_CUSTODY';
update history
set type='ESCAPE_FROM_ESCORT_1'
where type = 'ESCAPE_FROM_ESCORT';
update history
set type='FIND_6'
where type = 'FINDS';
update history
set type='FIRE_1'
where type = 'FIRE';
update history
set type='FOOD_REFUSAL_1'
where type = 'FOOD_REFUSAL';
update history
set type='CLOSE_DOWN_SEARCH_1'
where type = 'FULL_CLOSE_DOWN_SEARCH';
update history
set type='KEY_OR_LOCK_2'
where type = 'KEY_LOCK_INCIDENT';
update history
set type='MISCELLANEOUS_1'
where type = 'MISCELLANEOUS';
update history
set type='RADIO_COMPROMISE_1'
where type = 'RADIO_COMPROMISE';
update history
set type='RELEASE_IN_ERROR_1'
where type = 'RELEASED_IN_ERROR';
update history
set type='SELF_HARM_1'
where type = 'SELF_HARM';
update history
set type='TEMPORARY_RELEASE_FAILURE_4'
where type = 'TEMPORARY_RELEASE_FAILURE';
update history
set type='TOOL_LOSS_1'
where type = 'TOOL_LOSS';
update history
set type='DAMAGE_1'
where type = 'DAMAGE';
update history
set type='ASSAULT_1'
where type = 'OLD_ASSAULT';
update history
set type='ASSAULT_2'
where type = 'OLD_ASSAULT1';
update history
set type='ASSAULT_3'
where type = 'OLD_ASSAULT2';
update history
set type='ASSAULT_4'
where type = 'OLD_ASSAULT3';
update history
set type='BARRICADE_1'
where type = 'OLD_BARRICADE';
update history
set type='CONCERTED_INDISCIPLINE_1'
where type = 'OLD_CONCERTED_INDISCIPLINE';
update history
set type='DISORDER_1'
where type = 'OLD_DISORDER';
update history
set type='DRONE_SIGHTING_1'
where type = 'OLD_DRONE_SIGHTING';
update history
set type='DRONE_SIGHTING_2'
where type = 'OLD_DRONE_SIGHTING1';
update history
set type='DRUGS_1'
where type = 'OLD_DRUGS';
update history
set type='FIND_1'
where type = 'OLD_FINDS';
update history
set type='FIND_2'
where type = 'OLD_FINDS1';
update history
set type='FIND_3'
where type = 'OLD_FINDS2';
update history
set type='FIND_4'
where type = 'OLD_FINDS3';
update history
set type='FIND_5'
where type = 'OLD_FINDS4';
update history
set type='FIREARM_1'
where type = 'OLD_FIREARM_ETC';
update history
set type='HOSTAGE_1'
where type = 'OLD_HOSTAGE';
update history
set type='KEY_OR_LOCK_1'
where type = 'OLD_KEY_LOCK_INCIDENT';
update history
set type='MOBILE_PHONE_1'
where type = 'OLD_MOBILES';
update history
set type='INCIDENT_AT_HEIGHT_1'
where type = 'OLD_ROOF_CLIMB';
update history
set type='TEMPORARY_RELEASE_FAILURE_1'
where type = 'OLD_TEMPORARY_RELEASE_FAILURE';
update history
set type='TEMPORARY_RELEASE_FAILURE_2'
where type = 'OLD_TEMPORARY_RELEASE_FAILURE1';
update history
set type='TEMPORARY_RELEASE_FAILURE_3'
where type = 'OLD_TEMPORARY_RELEASE_FAILURE2';

update constant_status
set description='Information amended'
where code = 'INFORMATION_AMENDED';
