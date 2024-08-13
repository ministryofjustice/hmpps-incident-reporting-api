CREATE INDEX event_prison_idx on event (prison_id);
CREATE INDEX report_assigned_to_idx ON report (assigned_to);
CREATE INDEX report_reported_by_idx ON report (reported_by);
CREATE INDEX prisoner_involvement_prisoner_number_idx ON prisoner_involvement(prisoner_number);
CREATE INDEX staff_involvement_staff_username_idx ON staff_involvement(staff_username);
CREATE INDEX status_history_status_idx ON status_history (status);
