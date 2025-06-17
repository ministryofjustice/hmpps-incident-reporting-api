ALTER TABLE response ADD COLUMN code VARCHAR(60);
ALTER TABLE historical_response ADD COLUMN code VARCHAR(60);

CREATE INDEX response_code_idx on response (question_id, code);
