-- Removes INCIDENT_UPDATED status
-- See: https://dsdmoj.atlassian.net/wiki/spaces/IR/pages/5589893167/Status+old+and+new

DELETE FROM constant_status
WHERE code = 'INCIDENT_UPDATED';

