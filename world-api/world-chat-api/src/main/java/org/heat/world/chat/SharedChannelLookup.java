package org.heat.world.chat;

import com.github.blackrush.acara.EventBusBuilder;

import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

public final class SharedChannelLookup implements WorldChannelLookup {
    private final EventBusBuilder eventBusBuilder;
    private final Clock clock;

    private final Map<Integer, WorldChannel> channels = new HashMap<>();

    public SharedChannelLookup(EventBusBuilder eventBusBuilder, Clock clock) {
        this.eventBusBuilder = eventBusBuilder;
        this.clock = clock;
    }

    public void register(int channelId) {
        channels.put(channelId, new DedicatedWorldChannel(eventBusBuilder.build(), clock));
    }

    @Override
    public WorldChannel lookupChannel(WorldChannelMessage message) {
        WorldChannel channel = channels.get(message.getChannelId());

        if (channel == null) {
            throw new NoSuchElementException();
        }

        return channel;
    }
}
