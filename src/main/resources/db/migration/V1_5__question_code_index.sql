create index if not exists question_code_idx on question (code);
create index if not exists question_report_and_code_idx on question (report_id, code);

create index if not exists historical_question_code_idx on historical_question (code);
create index if not exists historical_question_history_and_code_idx on historical_question (history_id, code);
