package org.heat.world.chat;

public interface WorldChannelLookup {
    /**
     * Lookup a channel given a message
     * @param message a non-null message
     * @return a non-null channel
     * @throws java.util.NoSuchElementException if the lookup failed
     */
    WorldChannel lookupChannel(WorldChannelMessage message);
}
