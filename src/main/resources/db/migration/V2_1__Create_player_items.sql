create table player_items(
    player_id integer not null references players(id) on delete restrict,
    item_uid integer not null references items(uid) on delete cascade,
    primary key(player_id, item_uid)
);
