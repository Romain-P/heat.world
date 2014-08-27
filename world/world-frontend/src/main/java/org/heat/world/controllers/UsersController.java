package org.heat.world.controllers;

import com.ankamagames.dofus.datacenter.breeds.Breed;
import com.ankamagames.dofus.network.messages.game.approach.*;
import org.heat.User;
import org.heat.world.backend.Backend;
import org.heat.world.controllers.utils.Basics;
import org.heat.world.users.UserCapabilities;
import org.rocket.network.*;

import javax.inject.Inject;
import java.util.List;

@Controller
public class UsersController {
    @Inject NetworkClient client;
    @Inject Backend backend;
    @Inject MutProp<User> user;
    @Inject UserCapabilities capabilities;

    @Connect
    public void onConnect() {
        client.write(HelloGameMessage.i);
    }

    private short exportBreeds(List<Breed> breeds) {
        short res = 0;
        for (Breed breed : breeds) {
            res |= 1 << (breed.getId() - 1);
        }
        return res;
    }

    private short exportVisibleBreeds() {
        return exportBreeds(capabilities.getVisibleBreeds(user.get()));
    }

    private short exportAvailableBreeds() {
        return exportBreeds(capabilities.getAvailableBreeds(user.get()));
    }

    @PropValidation(value = User.class, present = false)
    @Receive
    public void authenticate(AuthenticationTicketMessage msg) {
        backend.authenticateUser(msg.ticket)
                .onSuccess(user::set)
                .flatMap(user -> client.transaction(tx -> {
                    tx.write(AuthenticationTicketAcceptedMessage.i);
                    tx.write(Basics.time());
                    // TODO(world/frontend): server lang, community and type
                    tx.write(new ServerSettingsMessage("fr", (byte) 0, (byte) 0));
                    tx.write(new AccountCapabilitiesMessage(
                            user.getId(),
                            capabilities.isTutorialAvailable(user),
                            exportVisibleBreeds(),
                            exportAvailableBreeds(),
                            (byte) 0 // TODO(world/frontend): status when authenticating
                    ));
                }))
                .mayRescue(cause -> client.write(AuthenticationTicketRefusedMessage.i).flatMap(x -> client.close()))
        ;
    }

    @Disconnect
    public void acknowledgeUserDisconnection() {
        if (user.isPresent()) {
            backend.acknowledgeDisconnection(user.get());
        }
    }
}
