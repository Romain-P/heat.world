package org.heat.world.chat;

public class StringChannelMessage implements WorldChannelMessage {
    private final String string;

    StringChannelMessage(String string) {
        this.string = string;
    }

    public String getString() {
        return string;
    }

    @Override
    public String render() {
        return string;
    }
}
