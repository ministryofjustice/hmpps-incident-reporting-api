-- Fix typo in FOOD_REFUSAL description

UPDATE constant_type_family
SET description='Food or liquid refusal'
WHERE code = 'FOOD_REFUSAL';

UPDATE constant_type
SET description='Food or liquid refusal'
WHERE family_code = 'FOOD_REFUSAL';
