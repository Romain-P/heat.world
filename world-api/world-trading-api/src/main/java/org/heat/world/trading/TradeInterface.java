package org.heat.world.trading;

import com.github.blackrush.acara.EventBus;
import org.heat.world.items.WorldBag;

import java.util.Optional;

public interface TradeInterface {
    EventBus getEventBus();

    Optional<? extends Result> conclude();
    boolean isConcluded();

    public interface Result {
        WorldBag getFirst();
        WorldBag getSecond();
    }
}
