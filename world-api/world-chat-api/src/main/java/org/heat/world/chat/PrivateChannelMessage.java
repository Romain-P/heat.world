package org.heat.world.chat;

public final class PrivateChannelMessage implements WorldChannelMessage {
    private final String receiver;
    private final WorldChannelMessage message;

    public PrivateChannelMessage(String receiver, WorldChannelMessage message) {
        this.receiver = receiver;
        this.message = message;
    }

    public String getReceiver() {
        return receiver;
    }

    public WorldChannelMessage getMessage() {
        return message;
    }

    @Override
    public String getString() {
        return message.getString();
    }
}
