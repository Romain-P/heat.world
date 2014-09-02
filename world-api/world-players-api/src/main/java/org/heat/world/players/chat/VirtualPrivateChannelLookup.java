package org.heat.world.players.chat;

import org.heat.world.chat.PrivateChannelMessage;
import org.heat.world.chat.WorldChannel;
import org.heat.world.chat.WorldChannelLookup;
import org.heat.world.chat.WorldChannelMessage;
import org.heat.world.players.PlayerRegistry;

public final class VirtualPrivateChannelLookup implements WorldChannelLookup {
    private final PlayerRegistry playerRegistry;
    private final WorldChannelLookup fallback;

    public VirtualPrivateChannelLookup(PlayerRegistry playerRegistry, WorldChannelLookup fallback) {
        this.playerRegistry = playerRegistry;
        this.fallback = fallback;
    }

    @Override
    public WorldChannel lookupChannel(WorldChannelMessage o) {
        if (o instanceof PrivateChannelMessage) {
            PrivateChannelMessage message = (PrivateChannelMessage) o;

            if (message instanceof PrivateChannelMessage.ByReceiverId) {
                PrivateChannelMessage.ByReceiverId byId = (PrivateChannelMessage.ByReceiverId) message;
                return playerRegistry.findPlayer(byId.getReceiverId()).get();
            } else if (message instanceof PrivateChannelMessage.ByReceiverName) {
                PrivateChannelMessage.ByReceiverName byName = (PrivateChannelMessage.ByReceiverName) message;
                return playerRegistry.findPlayerByName(byName.getReceiverName()).get();
            } else if (message instanceof PrivateChannelMessage.Resolved) {
                return message.getReceiver();
            }

            throw new Error("unhandlable private message " + message);
        }

        return fallback.lookupChannel(o);
    }
}
