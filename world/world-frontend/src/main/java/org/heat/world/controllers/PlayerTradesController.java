package org.heat.world.controllers;

import org.heat.world.players.Player;
import org.rocket.network.Controller;
import org.rocket.network.NetworkClient;
import org.rocket.network.Prop;

import javax.inject.Inject;

@Controller
public class PlayerTradesController {
    @Inject NetworkClient client;
    @Inject Prop<Player> player;
}
