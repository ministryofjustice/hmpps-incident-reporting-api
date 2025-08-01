ALTER TABLE report ADD COLUMN duplicated_report_id uuid
  CONSTRAINT original_report_fk REFERENCES report (id) ON DELETE SET NULL;
