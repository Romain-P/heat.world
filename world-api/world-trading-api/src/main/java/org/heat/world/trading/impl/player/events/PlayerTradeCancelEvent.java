package org.heat.world.trading.impl.player.events;

import org.heat.world.trading.impl.player.PlayerTrade;
import org.heat.world.trading.impl.player.PlayerTrader;

public final class PlayerTradeCancelEvent extends PlayerTradeEvent {
    private final PlayerTrader canceller;

    public PlayerTradeCancelEvent(PlayerTrade trade, PlayerTrader canceller) {
        super(trade);
        this.canceller = canceller;
    }

    public PlayerTrader getCanceller() {
        return canceller;
    }
}
