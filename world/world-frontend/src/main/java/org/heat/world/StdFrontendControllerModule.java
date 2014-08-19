package org.heat.world;

import org.heat.User;
import org.heat.world.controllers.*;
import org.heat.world.players.Player;
import org.heat.world.roleplay.WorldAction;
import org.rocket.network.guice.ControllerModule;

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

        newProp(User.class);
        newProp(Player.class);
        newProp(WorldAction.class);
    }
}
