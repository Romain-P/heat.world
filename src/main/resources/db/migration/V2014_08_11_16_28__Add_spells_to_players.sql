delete from players;

drop type if exists player_spell;
create type player_spell as (id int, level int, position int);

alter table players
    add spells player_spell[] not null
    ;