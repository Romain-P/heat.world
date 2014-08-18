package org.heat.world.items;

import com.ankamagames.dofus.network.enums.CharacterInventoryPositionEnum;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

public final class MapItemBag implements WorldItemBag {
    private final Map<Integer, WorldItem> map;

    private MapItemBag(Map<Integer, WorldItem> map) {
        this.map = map;
    }

    public static MapItemBag newHashMapItemBag() {
        return new MapItemBag(new HashMap<>());
    }

    public static MapItemBag newConcurrentHashMapItemBag() {
        return new MapItemBag(new ConcurrentHashMap<>());
    }

    @Override
    public Optional<WorldItem> findByUid(int uid) {
        return Optional.ofNullable(map.get(uid));
    }

    @Override
    public Stream<WorldItem> findByGid(int gid) {
        return map.values().stream().filter(x -> x.getTemplate().getId() == gid);
    }

    @Override
    public Stream<WorldItem> findByPosition(CharacterInventoryPositionEnum position) {
        requireNonNull(position, "position");
        return map.values().stream().filter(x -> x.getPosition() == position);
    }

    @Override
    public Stream<WorldItem> getItemStream() {
        return map.values().stream();
    }

    @Override
    public void add(WorldItem item) {
        if (map.containsKey(item.getUid())) {
            throw new IllegalArgumentException("bag already contains " + item);
        }

        map.put(item.getUid(), item);
    }

    @Override
    public void addAll(List<WorldItem> items) {
        items.forEach(this::add);
    }

    @Override
    public void update(WorldItem item) {
        WorldItem old = map.get(item.getUid());
        if (old == null) {
            throw new NoSuchElementException("bag does not contain " + item);
        }
        if (old.getVersion() > item.getVersion()) {
            throw new OutdatedItemException(item, old);
        }

        map.put(item.getUid(), item);
    }

    @Override
    public void remove(WorldItem item) {
        WorldItem actual = map.get(item.getUid());
        if (actual == null) {
            throw new NoSuchElementException("bag does not contain " + item);
        }
        if (actual.getVersion() != item.getVersion()) {
            throw new OutdatedItemException(item, actual);
        }

        map.remove(item.getUid());
    }

    @Override
    public Optional<WorldItem> tryRemove(int uid) {
        return Optional.ofNullable(map.remove(uid));
    }
}
