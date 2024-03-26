alter table incident_report add column source varchar(5) default 'DPS' not null;

CREATE SEQUENCE incident_number_sequence INCREMENT 1 START 1000000;