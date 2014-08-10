package org.heat.world.controllers;

import com.ankamagames.dofus.network.messages.game.friend.*;
import org.heat.world.controllers.utils.Authenticated;
import org.rocket.network.Controller;
import org.rocket.network.NetworkClient;
import org.rocket.network.Receive;

import javax.inject.Inject;
import java.util.stream.Stream;

@Controller
@Authenticated
public class FriendsController {
    @Inject NetworkClient client;

    // TODO(world/frontend): friends, ignored, spouse

    @Receive
    public void getFriendsList(FriendsGetListMessage msg) {
        client.write(new FriendsListMessage(Stream.empty()));
    }

    @Receive
    public void getIgnoredList(IgnoredGetListMessage msg) {
        client.write(new IgnoredListMessage(Stream.empty()));
    }

    @Receive
    public void getSpouseInfos(SpouseGetInformationsMessage msg) {
        client.write(new SpouseStatusMessage(false));
    }
}
