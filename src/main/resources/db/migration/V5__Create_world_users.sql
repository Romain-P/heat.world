create table world_users(id integer primary key);

delete from items;
delete from players;

drop index players_userid_key;

alter table players
  add constraint players_userid_fk foreign key (userId) references world_users(id)
  ;
