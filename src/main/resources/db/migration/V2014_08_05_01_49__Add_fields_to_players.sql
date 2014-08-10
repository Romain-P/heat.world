alter table players
    add breedId smallint not null,
    add sex boolean not null,
    add lookId smallint not null,
    add headId smallint not null,
    add colors int[] not null,
    add mapId int not null,
    add cellId smallint not null,
    add directionId smallint not null,
    add experience double precision not null
    ;