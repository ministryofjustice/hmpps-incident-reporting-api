UPDATE constant_type_family
  set sequence = sequence +1
WHERE sequence >= 12;

-- add new type
insert into constant_type_family(sequence, code, description)
values (12,  'DIRTY_PROTEST', 'Dirty protest');

UPDATE constant_type
set sequence = sequence +1
WHERE sequence >= 16;

insert into constant_type(sequence, code, description, family_code, active)
values(16, 'DIRTY_PROTEST_1', 'Dirty protest', 'DIRTY_PROTEST', true);


