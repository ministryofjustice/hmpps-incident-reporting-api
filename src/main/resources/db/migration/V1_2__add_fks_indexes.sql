CREATE INDEX report_event_id_fk_idx ON report(event_id);

CREATE INDEX question_report_id_fk_idx ON question(report_id);
CREATE INDEX response_question_id_fk_idx ON response(question_id);

CREATE INDEX history_report_id_fk_idx ON history(report_id);
CREATE INDEX historical_question_history_id_fk_idx ON historical_question(history_id);
CREATE INDEX historical_response_historical_question_id_fk_idx ON historical_response(historical_question_id);

CREATE INDEX prisoner_involvement_report_id_fk_idx ON prisoner_involvement(report_id);
CREATE INDEX staff_involvement_report_id_fk_idx ON staff_involvement(report_id);

CREATE INDEX correction_request_report_id_fk_idx ON correction_request(report_id);

CREATE INDEX status_history_report_id_fk_idx ON status_history(report_id);
