create index if not exists staff_involvement_report_sequence_idx on staff_involvement (report_id, sequence);
create index if not exists prisoner_involvement_report_sequence_idx on prisoner_involvement (report_id, sequence);
create index if not exists correction_request_report_sequence_idx on correction_request (report_id, sequence);
