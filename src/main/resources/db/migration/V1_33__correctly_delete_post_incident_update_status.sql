-- Re-removes POST_INCIDENT_UPDATE status which had in fact been deactivated in NOMIS correctly,
-- but foolishly NOT removed from `status_history` in migration 1.31
-- See: https://dsdmoj.atlassian.net/wiki/spaces/IR/pages/5589893167/Status+old+and+new

update report
set status='ON_HOLD'
where status = 'POST_INCIDENT_UPDATE';

update status_history
set status='ON_HOLD'
where status = 'POST_INCIDENT_UPDATE';

delete
from constant_status
where code = 'POST_INCIDENT_UPDATE';
