package org.heat.world.controllers.events.roleplay;

import com.ankamagames.dofus.network.enums.ChatActivableChannelsEnum;

public final class LookupChannelEvent {
    private final ChatActivableChannelsEnum chat;

    public LookupChannelEvent(ChatActivableChannelsEnum chat) {
        this.chat = chat;
    }

    public ChatActivableChannelsEnum getChat() {
        return chat;
    }
}
