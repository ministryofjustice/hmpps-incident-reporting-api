update constant_type_family set description = 'Business Continuity - Disruption to 3rd party supplier' where code = 'BC_DISRUPT_3RD_PTY';
update constant_type_family set description = 'Business Continuity - Fuel shortage' where code = 'BC_FUEL_SHORTAGE';
update constant_type_family set description = 'Business Continuity - Loss of access / egress' where code = 'BC_LOSS_ACCESS_EGRESS';
update constant_type_family set description = 'Business Continuity - Loss of communications & digital systems' where code = 'BC_LOSS_COMMS';
update constant_type_family set description = 'Business Continuity - Loss of utilities' where code = 'BC_LOSS_UTILS';
update constant_type_family set description = 'Business Continuity - Severe weather' where code = 'BC_SERV_WEATHER';
update constant_type_family set description = 'Business Continuity - Staff shortages' where code = 'BC_STAFF_SHORTAGES';
update constant_type_family set description = 'Business Continuity - Widespread illness' where code = 'BC_WIDESPREAD_ILLNESS';

update constant_type set description = 'Business Continuity - Disruption to 3rd party supplier' where code = 'BC_DISRUPT_3RD_PTY_1';
update constant_type set description = 'Business Continuity - Fuel shortage' where code = 'BC_FUEL_SHORTAGE_1';
update constant_type set description = 'Business Continuity - Loss of access / egress' where code = 'BC_LOSS_ACCESS_EGRESS_1';
update constant_type set description = 'Business Continuity - Loss of communications & digital systems' where code = 'BC_LOSS_COMMS_1';
update constant_type set description = 'Business Continuity - Loss of utilities' where code = 'BC_LOSS_UTILS_1';
update constant_type set description = 'Business Continuity - Severe weather' where code = 'BC_SERV_WEATHER_1';
update constant_type set description = 'Business Continuity - Staff shortages' where code = 'BC_STAFF_SHORTAGES_1';
update constant_type set description = 'Business Continuity - Widespread illness' where code = 'BC_WIDESPREAD_ILLNESS_1';

-- Resequence constants so UI/clients get a stable alphabetical ordering by description
-- (tie-breaker by code for determinism).
WITH ordered_families AS (
  SELECT
    code,
    ROW_NUMBER() OVER (ORDER BY LOWER(description), code) AS new_sequence
  FROM constant_type_family
)
UPDATE constant_type_family f
SET sequence = o.new_sequence - 1
FROM ordered_families o
WHERE f.code = o.code;

WITH ordered_types AS (
  SELECT
    code,
    ROW_NUMBER() OVER (ORDER BY LOWER(description), code) AS new_sequence
  FROM constant_type
)
UPDATE constant_type t
SET sequence = o.new_sequence - 1
FROM ordered_types o
WHERE t.code = o.code;
