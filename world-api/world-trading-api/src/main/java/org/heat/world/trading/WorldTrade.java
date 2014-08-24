package org.heat.world.trading;

import org.heat.world.items.WorldItemWallet;

import java.util.Optional;

public interface WorldTrade extends WorldItemTrade, WorldKamasTrade {
    @Override
    Optional<? extends Result> conclude();

    @Override
    WorldItemWallet getTradeBag(WorldTradeSide side);

    public interface Result extends WorldItemTrade.Result, WorldKamasTrade.Result {
        @Override
        WorldItemWallet getFirst();

        @Override
        WorldItemWallet getSecond();
    }
}
