alter table staff_involvement
    alter column first_name set not null,
    alter column last_name set not null;
alter table prisoner_involvement
    alter column first_name set not null,
    alter column last_name set not null;
