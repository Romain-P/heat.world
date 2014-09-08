package org.heat.world.chat;

import com.github.blackrush.acara.EventBus;

import java.time.Clock;
import java.time.Instant;

public final class DedicatedWorldChannel implements VirtualWorldChannel {
    private final int channelId;
    private final EventBus eventBus;
    private final Clock clock;

    public DedicatedWorldChannel(int channelId, EventBus eventBus, Clock clock) {
        this.channelId = channelId;
        this.eventBus = eventBus;
        this.clock = clock;
    }

    public int getChannelId() {
        return channelId;
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
