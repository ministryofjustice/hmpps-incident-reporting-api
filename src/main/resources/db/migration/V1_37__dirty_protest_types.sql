-- add new type
insert into constant_type_family(sequence, code, description)
select MAX(sequence)+1,  'DIRTY_PROTEST', 'Dirty protest'
FROM constant_type_family;

insert into constant_type(sequence, code, description, family_code, active)
select MAX(sequence)+1, 'DIRTY_PROTEST_1', 'Dirty protest', 'DIRTY_PROTEST', true
FROM constant_type;


