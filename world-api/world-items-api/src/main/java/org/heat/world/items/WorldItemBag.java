package org.heat.world.items;

import com.ankamagames.dofus.network.enums.CharacterInventoryPositionEnum;
import org.fungsi.Either;
import org.heat.shared.Pair;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static com.ankamagames.dofus.network.enums.CharacterInventoryPositionEnum.INVENTORY_POSITION_NOT_EQUIPED;

public interface WorldItemBag {
    // read operations
    /**
     * Find an item by its uid
     * @param uid an integer
     * @return a non-null option
     */
    Optional<WorldItem> findByUid(int uid);

    /**
     * Find multiple items by their gid
     * @param gid an integer
     * @return a non-null, non-leaking stream
     */
    Stream<WorldItem> findByGid(int gid);

    /**
     * Find multiple items by their position
     * @param position an non-null position
     * @return a non-null, non-leaking stream
     */
    Stream<WorldItem> findByPosition(CharacterInventoryPositionEnum position);

    /**
     * Return a stream of items
     * @return a non-null, non-leaking stream
     */
    Stream<WorldItem> getItemStream();

    /**
     * Fork an item with a given quantity
     * @param item a non-null item that'll be forked
     * @param quantity a positive quantity lower or equal to given item's quantity
     * @return either forked items or same item if fork wasn't necessary
     */
    Either<Pair<WorldItem, WorldItem>, WorldItem> fork(WorldItem item, int quantity);

    /**
     * Either merge an item or return the same item if there wasn't any item which was mergeable with on the given position
     * @param item a non-null item
     * @param position a non-null position
     * @return either the merged item or the same item
     */
    Either<WorldItem, WorldItem> mergeOn(WorldItem item, CharacterInventoryPositionEnum position);

    // write operations
    /**
     * Add an item to bag
     * @param item a non-null item
     * @throws java.lang.IllegalArgumentException if it already contains item
     */
    void add(WorldItem item);

    /**
     * Add multiple items to bag
     * @param items a non-null, non-leaking stream
     * @throws java.lang.IllegalArgumentException if it already contains one of stream's item
     */
    void addAll(List<WorldItem> items);

    /**
     * Update an item in bag
     * @param item a non-null updated item
     * @throws java.util.NoSuchElementException if it doesn't contain item
     * @throws org.heat.world.items.OutdatedItemException if item is outdated
     */
    void update(WorldItem item);

    /**
     * Remove an item from bag
     * @param item a non-null item
     * @throws java.util.NoSuchElementException if it doesn't contain item
     * @throws org.heat.world.items.OutdatedItemException if you try to remove a younger or older item
     */
    void remove(WorldItem item);

    /**
     * Try to remove an item from bag
     * @param uid item's uid
     * @return a non-null option
     */
    Optional<WorldItem> tryRemove(int uid);

    // defaults
    /**
     * Try to merge a given item to another non-equiped item
     * @param item a non-null item
     * @return either merged or same item
     */
    default Either<WorldItem, WorldItem> merge(WorldItem item) {
        return mergeOn(item, INVENTORY_POSITION_NOT_EQUIPED);
    }

    /**
     * Merge an item
     * @param item a non-null item
     * @return either merged or moved item
     */
    default Either<WorldItem, WorldItem> mergeOrMove(WorldItem item, CharacterInventoryPositionEnum position) {
        return mergeOn(item, position)
            .rightMap(nonMerged -> item.withPosition(position));
    }
}
