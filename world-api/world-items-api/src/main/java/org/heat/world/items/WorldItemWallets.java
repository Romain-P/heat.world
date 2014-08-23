package org.heat.world.items;

import com.ankamagames.dofus.network.enums.CharacterInventoryPositionEnum;
import org.fungsi.Either;
import org.heat.shared.Pair;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public final class WorldItemWallets {
    private WorldItemWallets() {}
    
    public static WorldItemWallet unmodifiable(final List<WorldItem> items, final int kamas) {
        return new Adapter() {
            @Override
            public Stream<WorldItem> getItemStream() {
                return items.stream();
            }

            @Override
            public int getKamas() {
                return kamas;
            }
        };
    }

    public static class Adapter implements WorldItemWallet {
        @Override
        public Optional<WorldItem> findByUid(int uid) {
            return getItemStream().filter(x -> x.getUid() == uid).findAny();
        }

        @Override
        public Stream<WorldItem> findByGid(int gid) {
            return getItemStream().filter(x -> x.getGid() == gid);
        }

        @Override
        public Stream<WorldItem> findByPosition(CharacterInventoryPositionEnum position) {
            return getItemStream().filter(x -> x.getPosition() == position);
        }

        @Override
        public Stream<WorldItem> getItemStream() { throw new UnsupportedOperationException(); }

        @Override
        public int getKamas() { throw new UnsupportedOperationException(); }

        @Override
        public void add(WorldItem item) { throw new UnsupportedOperationException(); }

        @Override
        public void addAll(List<WorldItem> items) { throw new UnsupportedOperationException(); }

        @Override
        public void update(WorldItem item) { throw new UnsupportedOperationException(); }

        @Override
        public void remove(WorldItem item) { throw new UnsupportedOperationException(); }

        @Override
        public Optional<WorldItem> tryRemove(int uid) { throw new UnsupportedOperationException(); }

        @Override
        public Either<Pair<WorldItem, WorldItem>, WorldItem> fork(WorldItem item, int quantity) { throw new UnsupportedOperationException(); }

        @Override
        public Either<WorldItem, WorldItem> tryMerge(WorldItem item) { throw new UnsupportedOperationException(); }

        @Override
        public Either<WorldItem, WorldItem> mergeOrMove(WorldItem item, CharacterInventoryPositionEnum position) { throw new UnsupportedOperationException(); }

        @Override
        public void setKamas(int kamas) { throw new UnsupportedOperationException(); }
    }
}
