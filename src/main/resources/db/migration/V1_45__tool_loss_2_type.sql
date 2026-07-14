-- add TOOL_LOSS_2 type (family TOOL_LOSS already exists)

-- make room after TOOL_LOSS_1
UPDATE constant_type
  set sequence = sequence + 1
WHERE sequence > (SELECT sequence FROM constant_type WHERE code = 'TOOL_LOSS_1');

-- new type, immediately after TOOL_LOSS_1, same family
INSERT INTO constant_type(sequence, code, description, family_code, active)
VALUES (
  (SELECT sequence + 1 FROM constant_type WHERE code = 'TOOL_LOSS_1'),
  'TOOL_LOSS_2', 'Tool or implement loss', 'TOOL_LOSS', true
);
