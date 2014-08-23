create table players(id integer primary key, userId integer not null, name varchar(30) not null unique);
create index players_userId_key ON players (userId);