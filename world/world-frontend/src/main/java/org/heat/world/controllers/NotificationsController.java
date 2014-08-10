package org.heat.world.controllers;

import com.ankamagames.dofus.network.messages.game.context.notification.NotificationUpdateFlagMessage;
import org.heat.world.controllers.utils.Basics;
import org.heat.world.controllers.utils.RolePlaying;
import org.rocket.network.Controller;
import org.rocket.network.NetworkClient;
import org.rocket.network.Receive;

import javax.inject.Inject;

@Controller
@RolePlaying
public class NotificationsController {
    @Inject NetworkClient client;

    // TODO(world/frontend): notifications

    @Receive
    public void updateNotificationFlag(NotificationUpdateFlagMessage msg) {
        client.write(Basics.noop());
    }
}
