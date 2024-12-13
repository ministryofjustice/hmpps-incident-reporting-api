alter table staff_involvement
    add column first_name varchar(255) null,
    add column last_name  varchar(255) null;
alter table prisoner_involvement
    add column first_name varchar(255) null,
    add column last_name  varchar(255) null;
