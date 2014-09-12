package org.heat.world;

import com.github.blackrush.acara.EventBusBuilder;
import com.google.common.collect.ImmutableList;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import org.heat.world.chat.SharedChannelLookup;
import org.heat.world.chat.WorldChannelLookup;
import org.heat.world.controllers.*;
import org.heat.world.groups.WorldGroup;
import org.heat.world.groups.chat.GroupChannelLookup;
import org.heat.world.players.Player;
import org.heat.world.players.PlayerRegistry;
import org.heat.world.players.chat.CurrentMapChannelLookup;
import org.heat.world.players.chat.VirtualPrivateChannelLookup;
import org.heat.world.roleplay.WorldAction;
import org.heat.world.users.WorldUser;
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
        newController().to(GroupsController.class);
        newController().to(ChatController.class);
        newController().to(ContactsController.class);
        newController().to(BasicsController.class);

        newProp(WorldUser.class);
        newProp(Player.class);
        newProp(WorldAction.class);
        newProp(WorldGroup.class, Names.named("main"));
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
    GroupChannelLookup provideGroupChannelLookup(@Named("main") Prop<WorldGroup> mainGroup) {
        return new GroupChannelLookup(mainGroup::tryGet);
    }

    @Provides
    WorldChannelLookup provideChannelLookup(
            SharedChannelLookup shared,
            VirtualPrivateChannelLookup virtualPrivate,
            CurrentMapChannelLookup currentMap,
            GroupChannelLookup group
    ) {
        return currentMap
            .andThen(group)
            .andThen(virtualPrivate)
            .andThen(shared);
    }
}
