DROP TABLE if exists description_addendum;

CREATE TABLE description_addendum
(
  id         SERIAL
    CONSTRAINT description_addendum_pk PRIMARY KEY,
  report_id  UUID         NOT NULL
    CONSTRAINT description_addendum_report_fk REFERENCES report (id) ON DELETE CASCADE,
  sequence   INTEGER      NOT NULL,
  "text"     TEXT         NOT NULL,
  created_at TIMESTAMP    NOT NULL,
  created_by VARCHAR(120) NOT NULL,
  first_name VARCHAR(255) NOT NULL,
  last_name  VARCHAR(255) NOT NULL
);

CREATE INDEX description_addendum_report_id_fk_idx on description_addendum (report_id);
create index if not exists description_addendum_report_sequence_idx on description_addendum (report_id, sequence);
