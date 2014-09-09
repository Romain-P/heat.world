package org.heat.world.controllers;

import com.ankamagames.dofus.network.enums.FightOptionsEnum;
import com.ankamagames.dofus.network.messages.game.context.fight.GameFightOptionToggleMessage;
import com.ankamagames.dofus.network.messages.game.context.roleplay.party.*;
import com.github.blackrush.acara.Listener;
import lombok.extern.slf4j.Slf4j;
import org.heat.world.controllers.events.ChoosePlayerEvent;
import org.heat.world.controllers.events.roleplay.chat.NewChannelEvent;
import org.heat.world.controllers.events.roleplay.chat.QuitChannelEvent;
import org.heat.world.controllers.utils.Basics;
import org.heat.world.controllers.utils.RolePlaying;
import org.heat.world.groups.*;
import org.heat.world.groups.events.*;
import org.heat.world.players.Player;
import org.heat.world.players.PlayerRegistry;
import org.rocket.network.*;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import static com.ankamagames.dofus.network.enums.PartyJoinErrorEnum.PARTY_JOIN_ERROR_NOT_ENOUGH_ROOM;
import static com.ankamagames.dofus.network.enums.PartyJoinErrorEnum.PARTY_JOIN_ERROR_PLAYER_NOT_FOUND;

@Slf4j
@Controller
@RolePlaying
public class GroupsController {
    @Inject NetworkClient client;
    @Inject Prop<Player> player;
    @Inject @Named("main") MutProp<WorldGroup> mainGroup;

    @Inject PlayerRegistry playerRegistry;
    @Inject WorldGroupFactory groupFactory;

    Map<Integer, WorldGroup> groups;
    Map<Integer, WorldGroup.Invitation> invitations;

    WorldGroup createGroup() {
        WorldGroup group = groupFactory.create(player.get());
        setGroup(group);
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

    WorldGroup getMainGroup() {
        return mainGroup.getOrSet(this::createGroup);
    }

    void setGroup(WorldGroup group) {
        if (groups == null) {
            groups = new HashMap<>();
        }
        groups.put(group.getGroupId(), group);
        group.getEventBus().subscribe(this);
        client.getEventBus().publish(new NewChannelEvent(group));
    }

    WorldGroup popGroup(int id) {
        WorldGroup group = groups.remove(id);
        if (group == null) {
            throw new IllegalArgumentException();
        }
        if (mainGroup.isPresent() && mainGroup.get().getGroupId() == id) {
            mainGroup.remove();
        }
        group.getEventBus().unsubscribe(this);
        client.getEventBus().publish(new QuitChannelEvent(group));
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

    @Disconnect
    public void onDisconnect() {
        if (groups != null) {
            for (WorldGroup group : groups.values()) {
                group.getEventBus().unsubscribe(this);
                client.getEventBus().publish(new QuitChannelEvent(group));

                group.leave(player.get());
            }

            groups.clear();
        }

        mainGroup.remove();
    }

    @Receive
    public void invite(PartyInvitationRequestMessage msg) {
        Optional<Player> option = playerRegistry.findPlayerByName(msg.name);
        if (!option.isPresent()) {
            client.write(new PartyCannotJoinErrorMessage(0, PARTY_JOIN_ERROR_PLAYER_NOT_FOUND.value));
            return;
        }

        Player player = this.player.get();
        WorldGroup group = getMainGroup();

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
    public void abdicateThrone(PartyAbdicateThroneMessage msg) {
        WorldGroup group = getGroup(msg.partyId);
        if (group.getLeader() != player.get()) {
            log.warn("you cannot abdicate if you are not yourself the leader {}", client);
            client.write(Basics.noop());
            return;
        }

        WorldGroupMember newLeader = group.findMember(msg.playerId).get();
        group.abdicateLeader(newLeader);
    }

    @Receive
    public void kickMember(PartyKickRequestMessage msg) {
        WorldGroup group = getGroup(msg.partyId);
        WorldGroupMember member = group.findMember(msg.playerId).get();
        group.kick(player.get(), member); // TODO(world/groups): verify permission to kick
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

    @Receive
    public void followMember(PartyFollowMemberRequestMessage msg) {
        WorldGroup group = getGroup(msg.partyId);

        // TODO(world/groups): follow member
        client.write(Basics.noop());
    }

    @Receive
    public void allFollowMember(PartyFollowThisMemberRequestMessage msg) {
        WorldGroup group = getGroup(msg.partyId);

        // TODO(world/groups): all follow member
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
            popGroup(evt.getGroup().getGroupId());
            client.write(new PartyKickedByMessage(evt.getGroup().getGroupId(), evt.getKicker().getActorId()));
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

    @Listener
    public void abdicate(AbdicateGroupEvent evt) {
        client.write(new PartyLeaderUpdateMessage(evt.getGroup().getGroupId(), evt.getNewLeader().getActorId()));
    }
}
