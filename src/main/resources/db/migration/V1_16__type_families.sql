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
