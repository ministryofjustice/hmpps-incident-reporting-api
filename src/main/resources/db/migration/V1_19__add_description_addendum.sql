CREATE TABLE description_addendum
(
  id                      SERIAL
    CONSTRAINT description_addendum_pk PRIMARY KEY,
  report_id               UUID         NOT NULL
    CONSTRAINT description_addendum_report_fk REFERENCES report (id) ON DELETE CASCADE,
  "text"     TEXT         NOT NULL,
  created_at TIMESTAMP    NOT NULL,
  created_by VARCHAR(120) NOT NULL
);

CREATE INDEX description_addendum_report_id_fk_idx on description_addendum (report_id);
CREATE INDEX description_addendum_created_at_idx on description_addendum (created_at);
