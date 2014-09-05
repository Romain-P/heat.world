package org.heat.world;

import com.github.blackrush.acara.EventBusBuilder;
import com.google.common.collect.ImmutableList;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.heat.User;
import org.heat.world.chat.SharedChannelLookup;
import org.heat.world.chat.WorldChannelLookup;
import org.heat.world.controllers.*;
import org.heat.world.players.Player;
import org.heat.world.players.PlayerRegistry;
import org.heat.world.players.chat.CurrentMapChannelLookup;
import org.heat.world.players.chat.VirtualPrivateChannelLookup;
import org.heat.world.roleplay.WorldAction;
import org.rocket.network.Prop;
import org.rocket.network.guice.ControllerModule;

import java.time.Clock;

import static com.ankamagames.dofus.network.enums.ChatChannelsMultiEnum.CHANNEL_SALES;
import static com.ankamagames.dofus.network.enums.ChatChannelsMultiEnum.CHANNEL_SEEK;

public class StdFrontendControllerModule extends ControllerModule {
    @Override
    protected void configure() {
        newController().to(UsersController.class);
        newController().to(PlayersController.class);
        newController().to(FriendsController.class);
        newController().to(PrismsController.class);
        newController().to(RolePlayController.class);
        newController().to(SecurityController.class);
        newController().to(NotificationsController.class);
        newController().to(ItemsController.class);
        newController().to(ShortcutsController.class);
        newController().to(PlayerTradesController.class);
        newController().to(ChatController.class);

        newProp(User.class);
        newProp(Player.class);
        newProp(WorldAction.class);
    }

    @Provides
    @Singleton
    SharedChannelLookup provideSharedChannelLookup(EventBusBuilder eventBusBuilder, Clock clock) {
        return new SharedChannelLookup(eventBusBuilder, clock, ImmutableList.of(
                (int) CHANNEL_SALES.value,
                (int) CHANNEL_SEEK.value
        ));
    }

    @Provides
    VirtualPrivateChannelLookup provideVirtualPrivateChannelLookup(Prop<Player> player, PlayerRegistry playerRegistry) {
        return new VirtualPrivateChannelLookup(player::get, playerRegistry);
    }

    @Provides
    CurrentMapChannelLookup provideCurrentMapChannelLookup(Prop<Player> player) {
        return new CurrentMapChannelLookup(player::get);
    }

    @Provides
    WorldChannelLookup provideChannelLookup(
            SharedChannelLookup shared,
            VirtualPrivateChannelLookup virtualPrivate,
            CurrentMapChannelLookup currentMap
    ) {
        return currentMap
            .andThen(virtualPrivate)
            .andThen(shared);
    }
}
