alter table constant_status
  add column ignore_downstream boolean not null default false;

update constant_status
set ignore_downstream = true
where code in ('DUPLICATE', 'NOT_REPORTABLE', 'REOPENED', 'WAS_CLOSED');
