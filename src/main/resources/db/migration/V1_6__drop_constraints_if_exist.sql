alter table question
    drop constraint if exists question_code_uniq;

alter table historical_question
    drop constraint if exists historical_question_code_uniq;
