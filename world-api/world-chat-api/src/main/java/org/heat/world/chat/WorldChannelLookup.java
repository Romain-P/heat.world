package org.heat.world.chat;

public interface WorldChannelLookup {
    /**
     * Lookup a channel given a message
     * @param message a non-null message
     * @return a <b>nullable</b> channel
     */
    WorldChannel lookupChannel(WorldChannelMessage message);

    /**
     * Compose this lookup with another one
     * @param fallback a non-null fallback
     * @return a non-null lookup
     */
    default WorldChannelLookup andThen(WorldChannelLookup fallback) {
        return message -> {
            WorldChannel channel = this.lookupChannel(message);
            if (channel == null) {
                channel = fallback.lookupChannel(message);
            }
            return channel;
        };
    }
}
