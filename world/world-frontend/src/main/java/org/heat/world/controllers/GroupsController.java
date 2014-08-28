package org.heat.world.controllers;

import com.github.blackrush.acara.Listener;
import org.heat.world.controllers.events.ChoosePlayerEvent;
import org.heat.world.controllers.utils.RolePlaying;
import org.heat.world.players.Player;
import org.rocket.network.Controller;
import org.rocket.network.NetworkClient;
import org.rocket.network.Prop;

import javax.inject.Inject;

@Controller
@RolePlaying
public class GroupsController {
    @Inject NetworkClient client;
    @Inject Prop<Player> player;

    @Listener
    public void listenPlayer(ChoosePlayerEvent evt) {
        evt.getPlayer().getEventBus().subscribe(this);
    }
}
