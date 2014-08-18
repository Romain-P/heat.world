package org.heat.world.players;

import com.ankamagames.dofus.network.enums.CharacterInventoryPositionEnum;
import org.heat.world.items.WorldItem;
import org.heat.world.items.WorldItemBag;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Stream;

public final class LazyPlayerItemWallet implements PlayerItemWallet {
    private final AtomicInteger kamas;
    private final int playerId;
    private final PlayerItemRepository playerItems;
    private final Supplier<WorldItemBag> bagSupplier;

    private volatile WorldItemBag bag;
    private final Object bagLock = new Object();

    public LazyPlayerItemWallet(int kamas, int playerId, PlayerItemRepository playerItems, Supplier<WorldItemBag> bagSupplier) {
        this.kamas = new AtomicInteger(kamas);
        this.playerId = playerId;
        this.playerItems = playerItems;
        this.bagSupplier = bagSupplier;
    }

    @Override
    public int getKamas() {
        return kamas.get();
    }

    @Override
    public void setKamas(int kamas) {
        this.kamas.set(kamas);
    }

    @Override
    public void plusKamas(int kamas) {
        this.kamas.getAndAdd(kamas);
    }

    private WorldItemBag loadBag() {
        // TODO(world/players): item load timeout
        List<WorldItem> items = playerItems.findItemsByPlayer(playerId).get();

        WorldItemBag bag = bagSupplier.get();
        bag.addAll(items);
        return bag;
    }

    private WorldItemBag loadBagIfNeeded() {
        // double-checked locking
        WorldItemBag result = bag;
        if (result == null) {
            synchronized (bagLock) {
                result = bag;
                if (result == null) {
                    result = bag = loadBag();
                }
            }
        }
        return result;
    }

    @Override
    public Optional<WorldItem> findByUid(int uid) {
        return loadBagIfNeeded().findByUid(uid);
    }

    @Override
    public Stream<WorldItem> findByGid(int gid) {
        return loadBagIfNeeded().findByGid(gid);
    }

    @Override
    public Stream<WorldItem> findByPosition(CharacterInventoryPositionEnum position) {
        return loadBagIfNeeded().findByPosition(position);
    }

    @Override
    public Stream<WorldItem> getItemStream() {
        return loadBagIfNeeded().getItemStream();
    }

    @Override
    public void add(WorldItem item) {
        loadBagIfNeeded().add(item);
        playerItems.persist(playerId, item);
    }

    @Override
    public void addAll(List<WorldItem> items) {
        loadBagIfNeeded().addAll(items);
        playerItems.persistAll(playerId, items.stream());
    }

    @Override
    public void update(WorldItem item) {
        loadBagIfNeeded().update(item);
        // NOTE(Blackrush): update only, no need to insert/delete tho
    }

    @Override
    public void remove(WorldItem item) {
        loadBagIfNeeded().remove(item);
        playerItems.remove(playerId, item);
    }

    @Override
    public Optional<WorldItem> tryRemove(int uid) {
        Optional<WorldItem> opt = loadBagIfNeeded().tryRemove(uid);
        opt.ifPresent(item -> playerItems.remove(playerId, item));
        return opt;
    }
}
