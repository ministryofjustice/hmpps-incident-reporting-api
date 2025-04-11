
create index if not exists self_harm_summary_idx
  on self_harm_summary (report_id, incident_date_and_time, location);
