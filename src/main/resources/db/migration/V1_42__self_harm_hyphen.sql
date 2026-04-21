-- Update Self-harm descriptions to have an hyphen

UPDATE constant_type
SET description='Self-harm'
WHERE code = 'SELF_HARM_1';

UPDATE constant_type_family
SET description='Self-harm'
WHERE code = 'SELF_HARM';
