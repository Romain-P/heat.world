package org.heat.world.chat;

import com.github.blackrush.acara.EventBus;
import org.heat.UserRank;

import java.time.Clock;
import java.time.Instant;

public final class DedicatedWorldChannel implements VirtualWorldChannel {
    private final EventBus eventBus;
    private final UserRank minRank;
    private final Clock clock;

    public DedicatedWorldChannel(EventBus eventBus, UserRank minRank, Clock clock) {
        this.eventBus = eventBus;
        this.minRank = minRank;
        this.clock = clock;
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @Override
    public boolean canSpeak(WorldSpeaker speaker) {
        return minRank.enough(speaker.getSpeakerRank());
    }

    @Override
    public Instant timestamp() {
        return clock.instant();
    }
}
