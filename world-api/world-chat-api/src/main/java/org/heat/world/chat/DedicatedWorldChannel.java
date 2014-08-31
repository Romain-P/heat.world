package org.heat.world.chat;

import com.github.blackrush.acara.EventBus;

import java.time.Clock;
import java.time.Instant;

public final class DedicatedWorldChannel implements VirtualWorldChannel {
    private final EventBus eventBus;
    private final Clock clock;

    public DedicatedWorldChannel(EventBus eventBus, Clock clock) {
        this.eventBus = eventBus;
        this.clock = clock;
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @Override
    public Instant timestamp() {
        return clock.instant();
    }
}
