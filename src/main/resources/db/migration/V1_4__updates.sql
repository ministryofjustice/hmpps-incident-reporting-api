ALTER TABLE incident_event DROP COLUMN summary;

ALTER TABLE incident_report ADD COLUMN question_set_id varchar(20);


ALTER TABLE incident_response RENAME COLUMN data_point_key TO data_item;
ALTER TABLE incident_response RENAME COLUMN comment TO additional_information;
ALTER TABLE incident_response DROP COLUMN recorded_on;
ALTER TABLE incident_response DROP COLUMN recorded_by;
ALTER TABLE incident_response ADD COLUMN data_item_description varchar(500);
ALTER TABLE incident_response ADD COLUMN sequence int not null default 0;

ALTER TABLE response RENAME COLUMN data_point_value TO item_value;
ALTER TABLE response RENAME COLUMN more_info TO additional_information;
ALTER TABLE response ADD COLUMN recorded_on timestamp NOT NULL default CURRENT_TIMESTAMP;
ALTER TABLE response ADD COLUMN recorded_by varchar(120) NOT NULL default 'system';
ALTER TABLE response ADD COLUMN sequence int not null default 0;


ALTER TABLE prisoner_involvement ADD COLUMN comment text;
ALTER TABLE prisoner_involvement ADD COLUMN outcome varchar(30);

ALTER TABLE staff_involvement ADD COLUMN comment text;

ALTER TABLE historical_incident_response RENAME COLUMN data_point_key TO data_item;
ALTER TABLE historical_incident_response RENAME COLUMN comment TO additional_information;
ALTER TABLE historical_incident_response DROP COLUMN recorded_on;
ALTER TABLE historical_incident_response DROP COLUMN recorded_by;
ALTER TABLE historical_incident_response ADD COLUMN data_item_description varchar(500);
ALTER TABLE historical_incident_response ADD COLUMN sequence int not null default 0;

ALTER TABLE historical_response RENAME COLUMN data_point_value TO item_value;
ALTER TABLE historical_response RENAME COLUMN more_info TO additional_information;
ALTER TABLE historical_response ADD COLUMN recorded_on timestamp NOT NULL default CURRENT_TIMESTAMP;
ALTER TABLE historical_response ADD COLUMN recorded_by varchar(120) NOT NULL default 'system';
ALTER TABLE historical_response ADD COLUMN sequence int not null default 0;