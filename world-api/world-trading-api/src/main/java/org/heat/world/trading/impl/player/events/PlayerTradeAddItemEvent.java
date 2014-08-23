package org.heat.world.trading.impl.player.events;

import org.heat.world.items.WorldItem;
import org.heat.world.trading.events.WorldItemTradeEvent;
import org.heat.world.trading.impl.player.PlayerTrade;
import org.heat.world.trading.impl.player.PlayerTrader;

public final class PlayerTradeAddItemEvent extends WorldItemTradeEvent {
    private final PlayerTrade trade;
    private final PlayerTrader trader;

    public PlayerTradeAddItemEvent(PlayerTrade trade, PlayerTrader trader, WorldItem item) {
        super(item);
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
