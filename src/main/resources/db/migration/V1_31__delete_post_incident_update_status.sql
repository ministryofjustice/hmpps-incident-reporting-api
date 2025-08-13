-- Removes POST_INCIDENT_UPDATE status which has now been removed from NOMIS
-- See: https://dsdmoj.atlassian.net/wiki/spaces/IR/pages/5589893167/Status+old+and+new

DELETE FROM constant_status
WHERE code = 'POST_INCIDENT_UPDATE';
