ALTER TABLE correction_request ADD COLUMN user_action varchar(60);
ALTER TABLE correction_request ADD COLUMN original_report_reference varchar(25);
ALTER TABLE correction_request ADD COLUMN user_type varchar(60);
