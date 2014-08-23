delete from items; -- will cascade delete player_items
delete from players; -- will cascade delete player_shortcuts

------------------------
-- created_at: when does the row has been created, never null
-- updated_at: when does the row has been updated, never null (same as created_at if never updated)
-- deleted_at: when does the row has been **soft** deleted, has a value only if it has been **soft** deleted

alter table players
    add created_at timestamp not null default CURRENT_TIMESTAMP,
    add updated_at timestamp not null,
    add deleted_at timestamp,

    -- when does the player has been last used
    add last_used_at timestamp not null
    ;

alter table items
    add created_at timestamp not null default CURRENT_TIMESTAMP,
    add updated_at timestamp not null,
    add deleted_at timestamp
    ;

alter table player_items -- hard-deleted to reduce garbage
    add created_at timestamp not null default CURRENT_TIMESTAMP
    ;

alter table player_shortcuts -- hard-deleted to reduce garbage
    add created_at timestamp not null default CURRENT_TIMESTAMP
    ;