-- add FOOD_REFUSAL_2 type (family FOOD_REFUSAL already exists)

-- make room after FOOD_REFUSAL_1
UPDATE constant_type
  set sequence = sequence + 1
WHERE sequence > (SELECT sequence FROM constant_type WHERE code = 'FOOD_REFUSAL_1');

-- new type, immediately after FOOD_REFUSAL_1, same family
INSERT INTO constant_type(sequence, code, description, family_code, active)
VALUES (
  (SELECT sequence + 1 FROM constant_type WHERE code = 'FOOD_REFUSAL_1'),
  'FOOD_REFUSAL_2', 'Food or liquid refusal', 'FOOD_REFUSAL', true
);
