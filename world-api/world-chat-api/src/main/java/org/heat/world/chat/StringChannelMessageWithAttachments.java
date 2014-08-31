package org.heat.world.chat;

import org.heat.world.items.WorldItem;

import java.util.List;

public final class StringChannelMessageWithAttachments extends StringChannelMessage {
    private final List<WorldItem> attachments;

    public StringChannelMessageWithAttachments(String string, List<WorldItem> attachments) {
        super(string);
        this.attachments = attachments;
    }

    public List<WorldItem> getAttachments() {
        return attachments;
    }
}
