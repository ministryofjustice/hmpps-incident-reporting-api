CREATE INDEX evidence_incident_report_fk_idx ON evidence (incident_id);
CREATE INDEX incident_correction_request_incident_report_fk_idx ON incident_correction_request (incident_id);
CREATE INDEX incident_report_incident_report_fk_idx ON incident_report (event_id);
CREATE INDEX incident_location_incident_report_fk_idx ON incident_location (incident_id);
CREATE INDEX other_person_involvement_incident_report_fk_idx ON other_person_involvement (incident_id);
CREATE INDEX prisoner_involvement_incident_report_fk_idx ON prisoner_involvement (incident_id);
CREATE INDEX staff_involvement_incident_report_fk_idx ON staff_involvement (incident_id);
CREATE INDEX status_history_incident_report_fk_idx ON status_history (incident_id);
CREATE INDEX incident_response_incident_fk_idx ON incident_response (incident_id);
CREATE INDEX response_incident_response_fk_idx ON response (incident_response_id);





