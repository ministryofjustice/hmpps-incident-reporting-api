ALTER TABLE question ADD COLUMN label TEXT;
ALTER TABLE historical_question ADD COLUMN label TEXT;
ALTER TABLE response ADD COLUMN label TEXT;
ALTER TABLE historical_response ADD COLUMN label TEXT;

update question set label = question;
update historical_question set label = question;
update response set label = response;
update historical_resposne set label = response;

ALTER TABLE question ALTER COLUMN label SET NOT NULL;
ALTER TABLE historical_question ALTER COLUMN label SET NOT NULL;
ALTER TABLE response ALTER COLUMN label SET NOT NULL;
ALTER TABLE historical_response ALTER COLUMN label SET NOT NULL;
