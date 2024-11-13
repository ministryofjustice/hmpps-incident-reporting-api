-- drop prison_id indices
drop index event_prison_idx;
drop index report_prison_id_idx;

-- rename fields
alter table event rename column prison_id to location;
alter table report rename column prison_id to location;

-- increase length from 6
alter table event alter column location type varchar(20);
alter table report alter column location type varchar(20);

-- recreate location indices
create index event_location_idx on event (location);
create index report_location_idx on report (location);
