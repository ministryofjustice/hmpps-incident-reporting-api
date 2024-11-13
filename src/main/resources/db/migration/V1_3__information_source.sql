alter table report add column modified_in varchar(5) default 'DPS' not null;

-- assume report has not been changed in a different system since creation
update report set modified_in = source;
