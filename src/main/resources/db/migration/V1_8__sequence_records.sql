-- Update correction_request sequences by report_id
WITH numbered_rows AS (
  SELECT id,
         ROW_NUMBER() OVER (PARTITION BY report_id ORDER BY correction_requested_at) - 1 as new_sequence
  FROM correction_request
)
UPDATE correction_request
SET sequence = numbered_rows.new_sequence
FROM numbered_rows
WHERE correction_request.id = numbered_rows.id;

-- Update staff_involvement sequences by report_id
WITH numbered_rows AS (
    SELECT id,
           ROW_NUMBER() OVER (PARTITION BY report_id ORDER BY staff_username, staff_role, comment) - 1 as new_sequence
    FROM staff_involvement
)
UPDATE staff_involvement
SET sequence = numbered_rows.new_sequence
FROM numbered_rows
WHERE staff_involvement.id = numbered_rows.id;

-- Update prisoner_involvement sequences by report_id
WITH numbered_rows AS (
    SELECT id,
           ROW_NUMBER() OVER (PARTITION BY report_id ORDER BY prisoner_number, prisoner_role, outcome) - 1 as new_sequence
    FROM prisoner_involvement
)
UPDATE prisoner_involvement
SET sequence = numbered_rows.new_sequence
FROM numbered_rows
WHERE prisoner_involvement.id = numbered_rows.id;