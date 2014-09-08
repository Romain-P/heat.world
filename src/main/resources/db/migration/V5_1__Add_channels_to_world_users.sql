delete from items;
delete from players;
delete from world_users;

alter table world_users
  add channels integer not null
  ;