package org.heat.world.trading;

import org.heat.world.items.WorldItem;
import org.heat.world.items.WorldItemBag;

import java.util.Optional;

public interface WorldItemTrade extends TradeInterface {
    void addItem(WorldTradeSide side, WorldItem item);
    void removeItem(WorldTradeSide side, WorldItem item);
    Optional<WorldItem> tryRemoveItem(WorldTradeSide side, int itemUid);

    @Override
    Optional<? extends Result> conclude();

    public interface Result extends TradeInterface.Result {
        @Override
        WorldItemBag getFirst();

        @Override
        WorldItemBag getSecond();
    }
}
