package org.heat.world.controllers;

import com.ankamagames.dofus.network.messages.game.social.ContactLookErrorMessage;
import com.ankamagames.dofus.network.messages.game.social.ContactLookMessage;
import com.ankamagames.dofus.network.messages.game.social.ContactLookRequestByIdMessage;
import org.heat.world.controllers.utils.RolePlaying;
import org.heat.world.players.Player;
import org.heat.world.players.PlayerRegistry;
import org.rocket.network.Controller;
import org.rocket.network.NetworkClient;
import org.rocket.network.Receive;

import javax.inject.Inject;
import java.util.Optional;

@Controller
@RolePlaying
public class ContactsController {
    @Inject NetworkClient client;

    @Inject PlayerRegistry playerRegistry;

    @Receive
    public void byId(ContactLookRequestByIdMessage msg) {
        Optional<Player> option = playerRegistry.findPlayer(msg.playerId);

        if (!option.isPresent()) {
            client.write(new ContactLookErrorMessage(msg.requestId));
            return;
        }

        Player contact = option.get();

        client.write(new ContactLookMessage(
            msg.requestId,
            contact.getName(),
            contact.getId(),
            contact.getLook().toEntityLook()));
    }
}
