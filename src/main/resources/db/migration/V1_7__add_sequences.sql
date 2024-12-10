alter table correction_request add column sequence integer default 0 not null;
alter table staff_involvement add column sequence integer default 0 not null;
alter table prisoner_involvement add column sequence integer default 0 not null;

