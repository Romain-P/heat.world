package org.heat.world.groups.impl;

import com.ankamagames.dofus.network.enums.PartyTypeEnum;
import com.github.blackrush.acara.EventBus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.heat.world.groups.WorldGroup;
import org.heat.world.groups.WorldGroupGuest;
import org.heat.world.groups.WorldGroupMember;
import org.heat.world.groups.events.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

final class ClassicalGroup implements WorldGroup {
    @Getter final int partyId;
    @Getter final EventBus eventBus;

    WorldGroupMember leader;
    final Map<Integer, WorldGroupMember> members = new HashMap<>();

    ClassicalGroup(int partyId, EventBus eventBus, WorldGroupMember leader) {
        this.partyId = partyId;
        this.eventBus = eventBus;
        this.leader = leader;
    }

    void disbandIfNeeded() {
        if (members.size() <= 0) {
            eventBus.publish(new DisbandGroupEvent(this));
        }
    }

    void addMember(WorldGroupMember member) {
        members.put(member.getActorId(), member);
    }

    void removeMember(WorldGroupMember member) {
        if (!members.remove(member.getActorId(), member)) {
            throw new IllegalArgumentException();
        }
    }

    void hasMember(WorldGroupMember member) {
        if (!members.containsKey(member.getActorId())) {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public PartyTypeEnum getPartyType() {
        return PartyTypeEnum.PARTY_TYPE_CLASSICAL;
    }

    @Override
    public WorldGroupMember getLeader() {
        return leader;
    }

    @Override
    public void abdicateLeader() {
        // TODO(world/groups): ClassicalGroup.abdicateLeader
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public Stream<WorldGroupMember> getMemberStream() {
        return members.values().stream();
    }

    @Override
    public Optional<WorldGroupMember> findMember(int memberId) {
        return Optional.ofNullable(members.get(memberId));
    }

    @Override
    public Invitation invite(WorldGroupMember inviter, WorldGroupMember guest) {
        return new Invit(new WorldGroupGuest(guest, inviter, Instant.now()));
    }

    @Override
    public void update(WorldGroupMember member) {
        hasMember(member);
        eventBus.publish(new UpdateGroupMemberEvent(this, member));
    }

    @Override
    public void leave(WorldGroupMember member) {
        removeMember(member);
        eventBus.publish(new LeaveGroupMemberEvent(this, member));
        disbandIfNeeded();
    }

    @Override
    public void kick(WorldGroupMember kicker, WorldGroupMember member) {
        removeMember(member);
        eventBus.publish(new KickGroupMemberEvent(this, member, kicker));
        disbandIfNeeded();
    }

    @RequiredArgsConstructor
    class Invit implements Invitation {
        @Getter final WorldGroupGuest groupGuest;

        @Override
        public void accept() {
            WorldGroupMember guest = groupGuest.getGuest();
            addMember(guest);
            eventBus.publish(new NewGroupMemberEvent(ClassicalGroup.this, guest));
        }

        @Override
        public void refuse() {
            eventBus.publish(new RemoveGuestGroupEvent(ClassicalGroup.this, groupGuest));
        }
    }
}
