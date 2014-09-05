package org.heat.world.players.chat;

import org.heat.world.chat.WorldChannel;
import org.heat.world.chat.WorldChannelLookup;
import org.heat.world.chat.WorldChannelMessage;
import org.heat.world.players.Player;

import java.util.NoSuchElementException;
import java.util.function.Supplier;

import static com.ankamagames.dofus.network.enums.ChatChannelsMultiEnum.CHANNEL_GLOBAL;

public final class CurrentMapChannelLookup implements WorldChannelLookup {
    private final Supplier<Player> player;

    private WorldChannelLookup fallback;

    public CurrentMapChannelLookup(Supplier<Player> player) {
        this.player = player;
        this.fallback = null;
    }

    public CurrentMapChannelLookup(Supplier<Player> player, WorldChannelLookup fallback) {
        this.player = player;
        this.fallback = fallback;
    }

    public CurrentMapChannelLookup then(WorldChannelLookup fallback) {
        this.fallback = fallback;
        return this;
    }

    @Override
    public WorldChannel lookupChannel(WorldChannelMessage message) {
        if (message.getChannelId() == CHANNEL_GLOBAL.value) {
            return player.get().getPosition().getMap();
        }

        if (fallback == null) {
            throw new NoSuchElementException();
        }
        return fallback.lookupChannel(message);
    }
}
