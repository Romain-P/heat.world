package org.heat.world.trading.impl.player;

import com.github.blackrush.acara.EventBus;

public final class PlayerTrades {
    private PlayerTrades() {}

    public static PlayerTrade create(EventBus eventBus, PlayerTrader first, PlayerTrader second) {
        return new PlayerTradeImpl(eventBus, first, second);
    }

    // Y U NO CURRY JAVA
    public static PlayerTradeFactory createFactory(EventBus eventBus) {
        return (first, second) -> create(eventBus, first, second);
    }
}
