package org.heat.world.trading.impl.player.events;

import org.heat.world.trading.events.WorldTradeEvent;
import org.heat.world.trading.impl.player.PlayerTrade;
import org.heat.world.trading.impl.player.PlayerTrader;

public final class PlayerTradeCheckEvent extends WorldTradeEvent {
    private final PlayerTrade trade;
    private final PlayerTrader trader;
    private final boolean check;

    public PlayerTradeCheckEvent(PlayerTrade trade, PlayerTrader trader, boolean check) {
        this.trade = trade;
        this.trader = trader;
        this.check = check;
    }

    @Override
    public PlayerTrade getTrade() {
        return trade;
    }

    public PlayerTrader getTrader() {
        return trader;
    }

    public boolean isCheck() {
        return check;
    }
}
