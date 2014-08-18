package org.heat.world.items;

import com.ankamagames.dofus.network.enums.CharacterInventoryPositionEnum;
import org.fungsi.Either;
import org.heat.shared.Pair;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public abstract class DelegateItemBag implements WorldItemBag {
    protected abstract WorldItemBag delegate();

    @Override
    public Optional<WorldItem> findByUid(int uid) {
        return delegate().findByUid(uid);
    }

    @Override
    public Stream<WorldItem> findByGid(int gid) {
        return delegate().findByGid(gid);
    }

    @Override
    public Stream<WorldItem> findByPosition(CharacterInventoryPositionEnum position) {
        return delegate().findByPosition(position);
    }

    @Override
    public Stream<WorldItem> getItemStream() {
        return delegate().getItemStream();
    }

    @Override
    public void add(WorldItem item) {
        delegate().add(item);
    }

    @Override
    public void addAll(List<WorldItem> items) {
        delegate().addAll(items);
    }

    @Override
    public void update(WorldItem item) {
        delegate().update(item);
    }

    @Override
    public void remove(WorldItem item) {
        delegate().remove(item);
    }

    @Override
    public Optional<WorldItem> tryRemove(int uid) {
        return delegate().tryRemove(uid);
    }

    @Override
    public Either<Pair<WorldItem, WorldItem>, WorldItem> fork(WorldItem item, int quantity) {
        return delegate().fork(item, quantity);
    }

    @Override
    public WorldItem merge(WorldItem item, CharacterInventoryPositionEnum position) {
        return delegate().merge(item, position);
    }

    @Override
    public boolean isValidMove(WorldItem item, CharacterInventoryPositionEnum to, int quantity) {
        return delegate().isValidMove(item, to, quantity);
    }
}
