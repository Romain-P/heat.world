package org.heat.world.trading.impl.player.events;

import org.heat.world.trading.events.WorldTradeEvent;
import org.heat.world.trading.impl.player.PlayerTrade;
import org.heat.world.trading.impl.player.PlayerTrader;

public final class PlayerTradeCancelEvent extends WorldTradeEvent {
    private final PlayerTrade trade;
    private final PlayerTrader canceller;

    public PlayerTradeCancelEvent(PlayerTrade trade, PlayerTrader canceller) {
        this.trade = trade;
        this.canceller = canceller;
    }

    @Override
    public PlayerTrade getTrade() {
        return trade;
    }

    public PlayerTrader getCanceller() {
        return canceller;
    }
}
