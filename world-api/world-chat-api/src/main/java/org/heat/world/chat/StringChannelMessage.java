package org.heat.world.chat;

public class StringChannelMessage implements WorldChannelMessage {
    private final String string;

    public StringChannelMessage(String string) {
        this.string = string;
    }

    @Override
    public String getString() {
        return string;
    }
}
