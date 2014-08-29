package org.heat.world.controllers;

import com.ankamagames.dofus.network.enums.FightOptionsEnum;
import com.ankamagames.dofus.network.messages.game.context.fight.GameFightOptionToggleMessage;
import com.ankamagames.dofus.network.messages.game.context.roleplay.party.*;
import com.github.blackrush.acara.Listener;
import org.heat.world.controllers.events.ChoosePlayerEvent;
import org.heat.world.controllers.utils.Basics;
import org.heat.world.controllers.utils.RolePlaying;
import org.heat.world.groups.*;
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
import java.util.NoSuchElementException;
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

    Map<Integer, WorldGroup> groups;
    Map<Integer, WorldGroup.Invitation> invitations;

    WorldGroup createGroup() {
        if (groups != null) {
            if (!groups.isEmpty()) {
                throw new IllegalStateException();
            }
        } else {
            groups = new HashMap<>();
        }

        WorldGroup group = groupFactory.create(player.get());
        group.getEventBus().subscribe(this);
        groups.put(group.getGroupId(), group);

        writePartyJoinMessage(group);

        return group;
    }

    WorldGroup getGroup(int id) {
        WorldGroup group = groups.get(id);
        if (group == null) {
            throw new NoSuchElementException();
        }
        return group;
    }

    void setGroup(WorldGroup group) {
        if (groups == null) {
            groups = new HashMap<>();
        }
        groups.put(group.getGroupId(), group);
    }

    WorldGroup popGroup(int id) {
        WorldGroup group = groups.remove(id);
        if (group == null) {
            throw new IllegalArgumentException();
        }
        group.getEventBus().unsubscribe(this);
        return group;
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
//        if (invitation == null) {
//            throw new IllegalArgumentException();
//        }
        return invitation;
    }

    void writePartyJoinMessage(WorldGroup group) {
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
        WorldGroup group = createGroup();

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

        invitation.getInvitationEndFuture()
            .onFailure(err -> {
                if (err instanceof WorldGroupInvitationCancelledException) {
                    WorldGroupInvitationCancelledException ex = (WorldGroupInvitationCancelledException) err;

                    popInvitation(group.getGroupId());

                    client.write(new PartyInvitationCancelledForGuestMessage(
                        group.getGroupId(),
                        ex.getCanceller().getActorId()));
                }
            });
    }

    @Receive
    public void cancelInvitation(PartyCancelInvitationMessage msg) {
        getGroup(msg.partyId).findInvitation(msg.guestId).get().cancel(player.get());
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
            writePartyJoinMessage(group);
            group.getEventBus().subscribe(this);
        } catch (WorldGroupMemberOverflowException e) {
            client.write(new PartyCannotJoinErrorMessage(msg.partyId, PARTY_JOIN_ERROR_NOT_ENOUGH_ROOM.value));
        }
    }

    @Receive
    public void refuse(PartyRefuseInvitationMessage msg) {
        WorldGroup.Invitation invitation = popInvitation(msg.partyId);
        if (invitation != null) {
            invitation.refuse();
            client.write(new PartyRefuseInvitationNotificationMessage(invitation.getGroup().getGroupId(), player.get().getId()));
        } else {
            // seems that you requested to view group details
            // still a bit buggy, just send a noop for now until i found out why it does not close the dialog
            client.write(Basics.noop());
        }
    }

    @Receive
    public void leave(PartyLeaveRequestMessage msg) {
        WorldGroup group = popGroup(msg.partyId);
        group.leave(player.get());
        client.write(new PartyLeaveMessage(group.getGroupId()));
    }

    @Receive
    public void setName(PartyNameSetRequestMessage msg) {
        WorldGroup group = getGroup(msg.partyId);
        group.setGroupName(msg.partyName);
    }

    @Receive
    public void toggleFightOption(GameFightOptionToggleMessage msg) {
        if (msg.option != FightOptionsEnum.FIGHT_OPTION_SET_TO_PARTY_ONLY.value) return;

        // TODO(world/groups): toggle fight option group only
        client.write(Basics.noop());
    }

    @Receive
    public void pledgeLoyalty(PartyPledgeLoyaltyRequestMessage msg) {
        WorldGroup group = getGroup(msg.partyId);

        // TODO(world/groups): pledge loyalty
        client.write(Basics.noop());
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
        client.write(new PartyRefuseInvitationNotificationMessage(evt.getGroup().getGroupId(), evt.getGuest().getGuest().getActorId()));
    }

    @Listener
    public void cancelGuest(CancelGuestGroupEvent evt) {
        client.write(new PartyCancelInvitationNotificationMessage(
                evt.getGroup().getGroupId(),
                evt.getCanceller().getActorId(),
                evt.getGuest().getGuest().getActorId()));
    }

    @Listener
    public void disbandGroup(DisbandGroupEvent evt) {
        popGroup(evt.getGroup().getGroupId());
        client.write(new PartyLeaveMessage(evt.getGroup().getGroupId()));
    }

    @Listener
    public void newName(NewNameGroupEvent evt) {
        client.write(new PartyNameUpdateMessage(evt.getGroup().getGroupId(), evt.getNewName()));
    }
}
