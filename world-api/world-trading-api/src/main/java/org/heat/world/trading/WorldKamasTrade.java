package org.heat.world.trading;

import org.heat.world.items.WorldWallet;

import java.util.Optional;

public interface WorldKamasTrade extends TradeInterface {

    @Override
    WorldWallet getTradeBag(WorldTradeSide side);

    @Override
    Optional<? extends Result> conclude();

    public interface Result extends TradeInterface.Result {
        @Override
        WorldWallet getFirst();

        @Override
        WorldWallet getSecond();
    }
}
