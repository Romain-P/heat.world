package org.heat.world.chat;

public interface WorldSpeaker {
    /**
     * Get speaker id
     * @return an int
     */
    int getSpeakerId();

    /**
     * Get speaker user id
     * @return an int
     */
    int getSpeakerUserId();

    /**
     * Get speaker name
     * @return a non-null {@link java.lang.String}
     */
    String getSpeakerName();
}
