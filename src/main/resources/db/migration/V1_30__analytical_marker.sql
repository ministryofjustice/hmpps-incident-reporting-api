CREATE TABLE analytical_marker
(
  response_code VARCHAR(60) not null,
  marker_type   varchar(60) not null,
  constraint analytical_marker_type_pk primary key (response_code, marker_type)
);

create unique index analytical_marker_pk on analytical_marker (response_code, marker_type);
