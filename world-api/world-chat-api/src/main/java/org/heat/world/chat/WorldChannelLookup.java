package org.heat.world.chat;

public interface WorldChannelLookup {
    /**
     * Lookup a channel given a channel id and a message
     * @param channelId an int representing a channel id
     * @param message a non-null message
     * @return a non-null channel
     * @throws java.util.NoSuchElementException if the lookup failed
     */
    WorldChannel lookupChannel(int channelId, WorldChannelMessage message);
}
