-- Add last_user_action column to report to capture the user action of the most recent correction request
ALTER TABLE report ADD COLUMN IF NOT EXISTS last_user_action varchar(60) NULL;

-- Backfill last_user_action with the user_action from the most recent correction_request per report
-- "Most recent" is determined by the highest sequence value per report (sequence starts at 0 and increases)
WITH latest AS (
  SELECT cr.report_id, cr.user_action
  FROM correction_request cr
  JOIN (
    SELECT report_id, MAX(sequence) AS max_seq
    FROM correction_request
    GROUP BY report_id
  ) m ON m.report_id = cr.report_id AND cr.sequence = m.max_seq
)
UPDATE report r
SET last_user_action = l.user_action
FROM latest l
WHERE r.id = l.report_id
AND r.status IN ('WAS_CLOSED', 'UPDATED', 'AWAITING_REVIEW');





