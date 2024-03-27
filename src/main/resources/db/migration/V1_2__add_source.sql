alter table incident_report add column source varchar(5) default 'DPS' not null;

alter table incident_report alter column incident_number type varchar(25);

CREATE SEQUENCE incident_number_sequence INCREMENT 1 START 1000000;

create unique index incident_report_incident_number_idx on incident_report(incident_number);