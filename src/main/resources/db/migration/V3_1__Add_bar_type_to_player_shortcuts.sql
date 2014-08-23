delete from player_shortcuts;

alter table player_shortcuts
    add column bar_type smallint not null,
    drop constraint player_shortcuts_pkey,
    add primary key (player_id, slot, bar_type)
    ;