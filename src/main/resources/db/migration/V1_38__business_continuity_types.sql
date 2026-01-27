ALTER TABLE constant_type_family ALTER COLUMN description TYPE varchar(255);
ALTER TABLE constant_type ALTER COLUMN description TYPE varchar(255);

UPDATE constant_type_family
  set sequence = sequence +8
WHERE sequence >= 5;

-- add new type
insert into constant_type_family(sequence, code, description)
values (5,  'BC_DISRUPT_3RD_PTY', 'Disruption to 3rd party supplier - Business Continuity'),
       (6, 'BC_FUEL_SHORTAGE', 'Fuel Shortage - Business Continuity'),
       (7, 'BC_LOSS_ACCESS_EGRESS', 'Loss of Access / Egress  - Business Continuity'),
       (8, 'BC_LOSS_COMMS', 'Loss of Communications & Digital Systems - Business Continuity'),
       (9, 'BC_LOSS_UTILS', 'Loss of Utilities - Business Continuity'),
       (10, 'BC_SERV_WEATHER', 'Severe Weather - Business Continuity'),
       (11, 'BC_STAFF_SHORTAGES', 'Staff shortages  - Business Continuity'),
       (12, 'BC_WIDESPREAD_ILLNESS', 'Widespread illness - Business Continuity');

UPDATE constant_type
set sequence = sequence +8
WHERE sequence >= 9;

insert into constant_type(sequence, code, description, family_code, active)
values(9, 'BC_DISRUPT_3RD_PTY_1', 'Disruption to 3rd party supplier - Business Continuity', 'BC_DISRUPT_3RD_PTY', true),
      (10, 'BC_FUEL_SHORTAGE_1', 'Fuel Shortage - Business Continuity', 'BC_FUEL_SHORTAGE', true),
      (11, 'BC_LOSS_ACCESS_EGRESS_1', 'Loss of Access / Egress  - Business Continuity', 'BC_LOSS_ACCESS_EGRESS', true),
      (12, 'BC_LOSS_COMMS_1', 'Loss of Communications & Digital Systems - Business Continuity', 'BC_LOSS_COMMS', true),
      (13, 'BC_LOSS_UTILS_1', 'Loss of Utilities - Business Continuity', 'BC_LOSS_UTILS', true),
      (14, 'BC_SERV_WEATHER_1', 'Severe Weather - Business Continuity', 'BC_SERV_WEATHER', true),
      (15, 'BC_STAFF_SHORTAGES_1', 'Staff shortages  - Business Continuity', 'BC_STAFF_SHORTAGES', true),
      (16, 'BC_WIDESPREAD_ILLNESS_1', 'Widespread illness - Business Continuity', 'BC_WIDESPREAD_ILLNESS', true);


