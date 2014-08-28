package org.heat.world.controllers;

import com.ankamagames.dofus.network.messages.game.context.roleplay.party.*;
import com.github.blackrush.acara.Listener;
import org.heat.world.controllers.events.ChoosePlayerEvent;
import org.heat.world.controllers.utils.RolePlaying;
import org.heat.world.groups.WorldGroup;
import org.heat.world.groups.WorldGroupMember;
import org.heat.world.players.Player;
import org.heat.world.players.PlayerRegistry;
import org.rocket.network.Controller;
import org.rocket.network.NetworkClient;
import org.rocket.network.Prop;
import org.rocket.network.Receive;

import javax.inject.Inject;
import java.util.Optional;

@Controller
@RolePlaying
public class GroupsController {
    @Inject NetworkClient client;
    @Inject Prop<Player> player;

    @Inject PlayerRegistry playerRegistry;

    WorldGroup getGroup() {
        // TODO(world/groups): GroupsController.getGroup
        throw new UnsupportedOperationException("not implemented");
    }

    void setGroup(WorldGroup gruop) {
        // TODO(world/groups): GroupsController.setGroup
        throw new UnsupportedOperationException("not implemented");
    }

    WorldGroup.Invitation getInvitation(int partyId) {
        // TODO(world/groups): GroupsController.getInvitation
        throw new UnsupportedOperationException("not implemented");
    }

    WorldGroup.Invitation popInvitation(int partyId) {
        // TODO(world/groups): GroupsController.popInvitation
        throw new UnsupportedOperationException("not implemented");
    }

    @Listener
    public void listenPlayer(ChoosePlayerEvent evt) {
        evt.getPlayer().getEventBus().subscribe(this);
    }

    @Receive
    public void invite(PartyInvitationRequestMessage msg) {
        Optional<Player> option = playerRegistry.findPlayerByName(msg.name);
        if (!option.isPresent()) {
            // send error
            return;
        }

        Player player = this.player.get();
        WorldGroup group = getGroup();

        Player target = option.get();
        WorldGroup.Invitation invitation = group.invite(player, target);

        target.getEventBus().publish(invitation);
        // no need to ack invitation receival
    }

    @Listener
    public void onInvitation(WorldGroup.Invitation invitation) {
        WorldGroup group = invitation.getGroup();
        WorldGroupMember inviter = invitation.getInviter();
        client.write(new PartyInvitationMessage(
                group.getGroupId(),
                group.getGroupType().value,
                "", // todo party name
                (byte) 0, // todo party max members
                inviter.getActorId(),
                inviter.getActorName(),
                player.get().getId()
        ));
    }

    @Receive
    public void getInvitationDetails(PartyInvitationDetailsRequestMessage msg) {
        WorldGroup.Invitation invitation = getInvitation(msg.partyId);
        WorldGroup group = invitation.getGroup();
        client.write(new PartyInvitationDetailsMessage(
                group.getGroupId(),
                group.getGroupType().value,
                "",
                invitation.getInviter().getActorId(),
                invitation.getInviter().getActorName(),
                group.getLeader().getActorId(),
                group.toPartyInvitationMemberInformations(),
                group.toPartyGuestInformations()
        ));
    }

    @Receive
    public void accept(PartyAcceptInvitationMessage msg) {
        WorldGroup.Invitation invitation = popInvitation(msg.partyId);
        WorldGroup group = invitation.getGroup();

        setGroup(group);
        group.getEventBus().subscribe(this);

        invitation.accept();
    }

    @Receive
    public void refuse(PartyRefuseInvitationMessage msg) {
        WorldGroup.Invitation invitation = popInvitation(msg.partyId);
        invitation.refuse();
    }
}
