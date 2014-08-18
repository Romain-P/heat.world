package org.heat.world.players;

import org.fungsi.Unit;
import org.fungsi.concurrent.Future;
import org.heat.world.items.WorldItem;

import java.util.List;

public interface PlayerItemRepository {
    Future<List<WorldItem>> findItemsByPlayer(int playerId);
    Future<Unit> persist(int playerId, int itemId);
    Future<Unit> remove(int playerId, int itemId);

    default Future<Unit> persist(Player player, WorldItem item) {
        return persist(player.getId(), item.getUid());
    }

    default Future<Unit> remove(Player player, WorldItem item) {
        return remove(player.getId(), item.getUid());
    }
}
