-- Re-add POST_INCIDENT_UPDATE status because it was not fully removed from NOMIS
-- See: https://dsdmoj.atlassian.net/wiki/spaces/IR/pages/5589893167/Status+old+and+new

insert into constant_status(sequence, code, description)
values (6, 'POST_INCIDENT_UPDATE', 'Post-incident update');
