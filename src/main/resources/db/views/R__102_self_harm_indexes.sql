
create index if not exists self_harm_summary_view_idx
  on self_harm_summary_view (report_id, incident_date_and_time, location);
