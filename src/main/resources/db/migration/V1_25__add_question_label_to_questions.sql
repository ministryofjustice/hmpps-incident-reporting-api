ALTER TABLE question ADD COLUMN label TEXT;
ALTER TABLE historical_question ADD COLUMN label TEXT;

update question set label = question;
update historical_question set label = question;

ALTER TABLE question ALTER COLUMN label SET NOT NULL;
ALTER TABLE historical_question ALTER COLUMN label SET NOT NULL;
