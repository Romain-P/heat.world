package org.heat.world.controllers;

import com.ankamagames.dofus.network.messages.game.inventory.exchanges.ExchangePlayerRequestMessage;
import org.heat.world.players.Player;
import org.rocket.network.Controller;
import org.rocket.network.NetworkClient;
import org.rocket.network.Prop;
import org.rocket.network.Receive;

import javax.inject.Inject;

@Controller
public class PlayerTradesController {
    @Inject NetworkClient client;
    @Inject Prop<Player> player;

    @Receive
    public void invite(ExchangePlayerRequestMessage msg) {

    }
}
