-- add KEY_OR_LOCK_3 type (family KEY_OR_LOCK already exists)

-- make room after KEY_OR_LOCK_2
UPDATE constant_type
  set sequence = sequence + 1
WHERE sequence > (SELECT sequence FROM constant_type WHERE code = 'KEY_OR_LOCK_2');

-- new type, immediately after KEY_OR_LOCK_2, same family
INSERT INTO constant_type(sequence, code, description, family_code, active)
VALUES (
  (SELECT sequence + 1 FROM constant_type WHERE code = 'KEY_OR_LOCK_2'),
  'KEY_OR_LOCK_3', 'Key or lock compromise', 'KEY_OR_LOCK', true
);
