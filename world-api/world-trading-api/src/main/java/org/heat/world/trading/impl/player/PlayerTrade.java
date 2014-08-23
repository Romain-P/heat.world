package org.heat.world.trading.impl.player;

import org.heat.world.trading.WorldTrade;
import org.heat.world.trading.WorldTradeSide;

import java.util.Optional;

public interface PlayerTrade extends WorldTrade {
    @Override
    Optional<? extends Result> conclude();
    void cancel(WorldTradeSide side);
    boolean isCancelled();

    void check(WorldTradeSide side);
    void uncheck(WorldTradeSide side);
}
