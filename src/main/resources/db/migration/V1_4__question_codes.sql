-- enforces unique question codes within one report
alter table question add constraint question_code_uniq unique (report_id, code);

-- enforces unique historical question codes within one report’s history item
alter table historical_question add constraint historical_question_code_uniq unique (history_id, code);
