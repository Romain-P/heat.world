package org.heat.world.controllers;

import com.ankamagames.dofus.network.messages.game.shortcut.ShortcutBarAddRequestMessage;
import com.ankamagames.dofus.network.messages.game.shortcut.ShortcutBarRefreshMessage;
import org.heat.world.controllers.utils.Basics;
import org.heat.world.controllers.utils.Idling;
import org.heat.world.players.Player;
import org.rocket.network.Controller;
import org.rocket.network.NetworkClient;
import org.rocket.network.Prop;
import org.rocket.network.Receive;

import javax.inject.Inject;

@Controller
@Idling
public class ShortcutsController {
    @Inject NetworkClient client;
    @Inject Prop<Player> player;

    @Receive
    public void addToShortcut(ShortcutBarAddRequestMessage msg) {
        client.transaction(tx -> {
            tx.write(new ShortcutBarRefreshMessage());
            tx.write(Basics.noop());
        });
    }
}
