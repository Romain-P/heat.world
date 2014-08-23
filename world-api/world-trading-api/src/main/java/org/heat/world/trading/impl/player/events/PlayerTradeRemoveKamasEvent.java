package org.heat.world.trading.impl.player.events;

import org.heat.world.trading.events.WorldKamasTradeEvent;
import org.heat.world.trading.impl.player.PlayerTrade;
import org.heat.world.trading.impl.player.PlayerTrader;

public final class PlayerTradeRemoveKamasEvent extends WorldKamasTradeEvent {
    private final PlayerTrade trade;
    private final PlayerTrader trader;

    public PlayerTradeRemoveKamasEvent(PlayerTrade trade, PlayerTrader trader, int kamas) {
        super(kamas);
        this.trader = trader;
        this.trade = trade;
    }

    @Override
    public PlayerTrade getTrade() {
        return trade;
    }

    @Override
    public PlayerTrader getTrader() {
        return trader;
    }
}
