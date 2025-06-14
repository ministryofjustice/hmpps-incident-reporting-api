-- Renames statuses and adds a few new ones
-- See: https://dsdmoj.atlassian.net/wiki/spaces/IR/pages/5589893167/Status+old+and+new


-- Renamed existing statuses
-- AWAITING_ANALYSIS => AWAITING_REVIEW
UPDATE constant_status
SET code='AWAITING_REVIEW',
    description='Awaiting review'
WHERE code = 'AWAITING_ANALYSIS';

-- INFORMATION_REQUIRED => NEEDS_UPDATING
UPDATE constant_status
SET code='NEEDS_UPDATING',
    description='Needs updating'
WHERE code = 'INFORMATION_REQUIRED';

-- INFORMATION_AMENDED => UPDATED
UPDATE constant_status
SET code='UPDATED',
    description='Updated'
WHERE code = 'INFORMATION_AMENDED';

-- IN_ANALYSIS => ON_HOLD
UPDATE constant_status
SET code='ON_HOLD',
    description='On hold'
WHERE code = 'IN_ANALYSIS';


-- Add new statuses
INSERT INTO constant_status(sequence, code, description)
VALUES (9, 'NOT_REPORTABLE', 'Not reportable'),
       (10, 'REOPENED', 'Reopened'),
       (11, 'WAS_CLOSED', 'Was closed');


-- Update all reports and histories with new statuses
-- AWAITING_ANALYSIS => AWAITING_REVIEW
UPDATE report
SET status='AWAITING_REVIEW'
WHERE status = 'AWAITING_ANALYSIS';
UPDATE status_history
SET status='AWAITING_REVIEW'
WHERE status = 'AWAITING_ANALYSIS';

-- INFORMATION_REQUIRED => NEEDS_UPDATING
UPDATE report
SET status='NEEDS_UPDATING'
WHERE status = 'INFORMATION_REQUIRED';
UPDATE status_history
SET status='NEEDS_UPDATING'
WHERE status = 'INFORMATION_REQUIRED';

-- INFORMATION_AMENDED => UPDATED
UPDATE report
SET status='UPDATED'
WHERE status = 'INFORMATION_AMENDED';
UPDATE status_history
SET status='UPDATED'
WHERE status = 'INFORMATION_AMENDED';

-- IN_ANALYSIS => ON_HOLD
UPDATE report
SET status='ON_HOLD'
WHERE status = 'IN_ANALYSIS';
UPDATE status_history
SET status='ON_HOLD'
WHERE status = 'IN_ANALYSIS';
