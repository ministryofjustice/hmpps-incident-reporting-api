-- removes events that grouped reports related to one incident

drop index if exists report_event_id_fk_idx;
alter table report
  drop column if exists event_id;
drop table if exists event;
drop sequence if exists event_sequence;
