package org.heat.world.controllers;

import com.ankamagames.dofus.network.messages.game.context.roleplay.party.*;
import com.github.blackrush.acara.Listener;
import org.heat.world.controllers.events.ChoosePlayerEvent;
import org.heat.world.controllers.utils.RolePlaying;
import org.heat.world.groups.WorldGroup;
import org.heat.world.groups.WorldGroupFactory;
import org.heat.world.groups.WorldGroupMember;
import org.heat.world.groups.WorldGroupMemberOverflowException;
import org.heat.world.groups.events.*;
import org.heat.world.players.Player;
import org.heat.world.players.PlayerRegistry;
import org.rocket.network.Controller;
import org.rocket.network.NetworkClient;
import org.rocket.network.Prop;
import org.rocket.network.Receive;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.ankamagames.dofus.network.enums.PartyJoinErrorEnum.PARTY_JOIN_ERROR_NOT_ENOUGH_ROOM;
import static com.ankamagames.dofus.network.enums.PartyJoinErrorEnum.PARTY_JOIN_ERROR_PLAYER_NOT_FOUND;

@Controller
@RolePlaying
public class GroupsController {
    @Inject NetworkClient client;
    @Inject Prop<Player> player;

    @Inject PlayerRegistry playerRegistry;
    @Inject WorldGroupFactory groupFactory;

    WorldGroup group;
    Map<Integer, WorldGroup.Invitation> invitations;

    WorldGroup getGroup() {
        if (group == null) {
            group = groupFactory.create(player.get());
            writePartyJoinMessage();
        }
        return group;
    }

    void setGroup(WorldGroup group) {
        this.group = group;
    }

    void pushInvitation(WorldGroup.Invitation invitation) {
        if (invitations == null) {
            invitations = new HashMap<>();
        }
        invitations.put(invitation.getGroup().getGroupId(), invitation);
    }

    WorldGroup.Invitation getInvitation(int partyId) {
        if (invitations == null) {
            throw new IllegalStateException();
        }
        WorldGroup.Invitation invitation = invitations.get(partyId);
        if (invitation == null) {
            throw new IllegalArgumentException();
        }
        return invitation;
    }

    WorldGroup.Invitation popInvitation(int partyId) {
        if (invitations == null) {
            throw new IllegalStateException();
        }
        WorldGroup.Invitation invitation = invitations.remove(partyId);
        if (invitation == null) {
            throw new IllegalArgumentException();
        }
        return invitation;
    }

    void writePartyJoinMessage() {
        client.write(new PartyJoinMessage(
                group.getGroupId(),
                group.getGroupType().value,
                group.getLeader().getActorId(),
                (byte) group.getMaxMembers(),
                group.toPartyMemberInformations(),
                group.toPartyGuestInformations(),
                false, // todo restriction
                group.getGroupName()
        ));
    }

    @Listener
    public void listenPlayer(ChoosePlayerEvent evt) {
        evt.getPlayer().getEventBus().subscribe(this);
    }

    @Receive
    public void invite(PartyInvitationRequestMessage msg) {
        Optional<Player> option = playerRegistry.findPlayerByName(msg.name);
        if (!option.isPresent()) {
            client.write(new PartyCannotJoinErrorMessage(0, PARTY_JOIN_ERROR_PLAYER_NOT_FOUND.value));
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
        pushInvitation(invitation);
        WorldGroup group = invitation.getGroup();
        WorldGroupMember inviter = invitation.getInviter();
        client.write(new PartyInvitationMessage(
                group.getGroupId(),
                group.getGroupType().value,
                group.getGroupName(),
                (byte) group.getMaxMembers(),
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
                group.getGroupName(),
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

        try {
            invitation.accept();
            setGroup(group);
            writePartyJoinMessage();
            group.getEventBus().subscribe(this);
        } catch (WorldGroupMemberOverflowException e) {
            client.write(new PartyCannotJoinErrorMessage(msg.partyId, PARTY_JOIN_ERROR_NOT_ENOUGH_ROOM.value));
        }
    }

    @Receive
    public void refuse(PartyRefuseInvitationMessage msg) {
        WorldGroup.Invitation invitation = popInvitation(msg.partyId);
        invitation.refuse();
    }

    @Listener
    public void newMember(NewGroupMemberEvent evt) {
        client.write(new PartyNewMemberMessage(evt.getGroup().getGroupId(), evt.getMember().toPartyMemberInformations()));
    }

    @Listener
    public void updateMember(UpdateGroupMemberEvent evt) {
        client.write(new PartyUpdateMessage(evt.getGroup().getGroupId(), evt.getMember().toPartyMemberInformations()));
    }

    @Listener
    public void leaveMember(LeaveGroupMemberEvent evt) {
        client.write(new PartyMemberRemoveMessage(evt.getGroup().getGroupId(), evt.getMember().getActorId()));
    }

    @Listener
    public void kickMember(KickGroupMemberEvent evt) {
        Player player = this.player.get();

        if (evt.getMember() == player) {
            // TODO(world/groups): GET REKT
        } else {
            client.write(new PartyMemberRemoveMessage(evt.getGroup().getGroupId(), evt.getMember().getActorId()));
        }
    }

    @Listener
    public void newGuest(NewGuestGroupEvent evt) {
        client.write(new PartyNewGuestMessage(evt.getGroup().getGroupId(), evt.getGuest().toPartyGuestInformations()));
    }

    @Listener
    public void removeGuest(RemoveGuestGroupEvent evt) {
        // TODO(world/groups): removeGuest
    }
}
