package org.heat.world.chat;

import com.github.blackrush.acara.EventBusBuilder;

import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SharedChannelLookup implements WorldChannelLookup {
    private final Map<Integer, WorldChannel> channels;

    public SharedChannelLookup(Map<Integer, WorldChannel> channels) {
        this.channels = channels;
    }

    public SharedChannelLookup(EventBusBuilder eventBusBuilder, Clock clock, List<Integer> channelIds) {
        this.channels = new HashMap<>();
        for (Integer channelId : channelIds) {
            this.channels.put(channelId, new DedicatedWorldChannel(eventBusBuilder.build(), clock));
        }
    }

    @Override
    public WorldChannel lookupChannel(WorldChannelMessage message) {
        return channels.get(message.getChannelId());
    }
}
