create table items(
    uid integer not null primary key,
    gid integer not null,
    effects bytea not null,
    position smallint not null,
    quantity integer not null
);
