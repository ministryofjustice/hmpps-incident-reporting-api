-- add new type
insert into constant_type_family(sequence, code, description)
values ((select max(sequence)+1 from constant_type_family),  'UNLAWFUL_DETENTION', 'Unlawful detention');

insert into constant_type(sequence, code, description, family_code, active)
values((select max(sequence)+1 from constant_type), 'UNLAWFUL_DETENTION_1', 'Unlawful detention', 'UNLAWFUL_DETENTION', true);

insert into constant_prisoner_role(sequence, code, description)
values ((select max(sequence)+1 from constant_prisoner_role), 'UNLAWFUL_DETENTION', 'Unlawful detention');
