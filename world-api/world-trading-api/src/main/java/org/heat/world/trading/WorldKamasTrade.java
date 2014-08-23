package org.heat.world.trading;

import org.heat.world.items.WorldWallet;

import java.util.Optional;

public interface WorldKamasTrade extends TradeInterface {
    void addKamas(WorldTradeSide side, int kamas);
    void removeKamas(WorldTradeSide side, int kamas);

    @Override
    Optional<? extends Result> conclude();

    public interface Result extends TradeInterface.Result {
        @Override
        WorldWallet getFirst();

        @Override
        WorldWallet getSecond();
    }
}
