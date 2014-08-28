package org.heat.world.groups.impl;

import com.ankamagames.dofus.network.enums.PartyTypeEnum;
import com.github.blackrush.acara.EventBus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.heat.world.groups.WorldGroup;
import org.heat.world.groups.WorldGroupGuest;
import org.heat.world.groups.WorldGroupMember;
import org.heat.world.groups.events.LeaveGroupMemberEvent;
import org.heat.world.groups.events.UpdateGroupMemberEvent;
import org.heat.world.roleplay.WorldHumanoidActor;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
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
    public Invitation invite(WorldGroupMember inviter, WorldHumanoidActor guest) {
        return new Invit(new WorldGroupGuest(guest, inviter, Instant.now()));
    }

    @Override
    public void update(WorldGroupMember member) {
        if (!members.containsKey(member.getActorId())) {
            throw new IllegalArgumentException();
        }

        eventBus.publish(new UpdateGroupMemberEvent(this, member));
    }

    @Override
    public void leave(WorldGroupMember member) {
        if (members.remove(member.getActorId(), member)) {
            eventBus.publish(new LeaveGroupMemberEvent(this, member));
        }
    }

    @Override
    public void kick(WorldGroupMember member) {
        // TODO(world/groups): ClassicalGroup.kick
        throw new UnsupportedOperationException("not implemented");
    }

    @RequiredArgsConstructor
    class Invit implements Invitation {
        @Getter final WorldGroupGuest groupGuest;

        @Override
        public void accept() {
            // TODO(world/groups): Invit.accept
            throw new UnsupportedOperationException("not implemented");
        }

        @Override
        public void refuse() {
            // TODO(world/groups): Invit.refuse
            throw new UnsupportedOperationException("not implemented");
        }
    }
}
