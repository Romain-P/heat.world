package org.heat.world.items;

import com.ankamagames.dofus.network.enums.CharacterInventoryPositionEnum;
import org.fungsi.Either;
import org.heat.shared.Pair;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static com.ankamagames.dofus.network.enums.CharacterInventoryPositionEnum.INVENTORY_POSITION_NOT_EQUIPED;
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

    @Override
    public Either<Pair<WorldItem, WorldItem>, WorldItem> fork(WorldItem item, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive but was equal to " + quantity);
        }
        if (quantity > item.getQuantity()) {
            throw new IllegalArgumentException("quantity must be lower or equal to " + item.getQuantity() + " but was equal to " + quantity);
        }

        if (quantity == item.getQuantity()) {
            return Either.right(item);
        }

        WorldItem forked = item.fork(quantity);
        return Either.left(Pair.of(item, forked));
    }

    @Override
    public WorldItem merge(WorldItem item, CharacterInventoryPositionEnum position) {
        requireNonNull(item, "item");

        Optional<WorldItem> opt = findByPosition(position)
                .filter(x -> WorldItem.compare(item, x) == 0)
                .findFirst();

        if (!opt.isPresent()) {
            return item.withPosition(position);
        }

        WorldItem same = opt.get();
        return same.plusQuantity(item.getQuantity());
    }


    @Override
    public boolean isValidMove(WorldItem item, CharacterInventoryPositionEnum to, int quantity) {
        /**
         * TODO(world/items): item movement validity
         * you cannot equip a ring twice
         * you cannot equip a pet if there is a mount
         * you cannot equip a greater level item
         * you cannot equip if target position is already taken
         */

        if (to == INVENTORY_POSITION_NOT_EQUIPED) {
            return true;
        }

        switch (item.getPosition()) {
            case ACCESSORY_POSITION_HAT:
            case ACCESSORY_POSITION_CAPE:
            case ACCESSORY_POSITION_BELT:
            case ACCESSORY_POSITION_BOOTS:
            case ACCESSORY_POSITION_AMULET:
            case ACCESSORY_POSITION_SHIELD:
            case ACCESSORY_POSITION_WEAPON:
            case INVENTORY_POSITION_RING_LEFT:
            case INVENTORY_POSITION_RING_RIGHT:
            case INVENTORY_POSITION_DOFUS_1:
            case INVENTORY_POSITION_DOFUS_2:
            case INVENTORY_POSITION_DOFUS_3:
            case INVENTORY_POSITION_DOFUS_4:
            case INVENTORY_POSITION_DOFUS_5:
            case INVENTORY_POSITION_DOFUS_6:
            case ACCESSORY_POSITION_PETS:
                return quantity == 1;

            default:
                return false;
        }
    }
}
