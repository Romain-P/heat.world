create table player_shortcuts(
    player_id integer references players(id) on delete cascade,
    slot integer not null,

    -- quick and dirty """polymorphism"""
    --### each "implementation" is compound of a unique `type` and a nullable column

    type varchar(10) not null check (type IN ('item', 'spell')),

    -- items
    item_uid integer references items(uid) on delete cascade,

    -- spells
    spell_id integer,

    primary key (player_id, slot)
);