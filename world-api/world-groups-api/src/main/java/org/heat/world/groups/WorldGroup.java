package org.heat.world.groups;

import com.ankamagames.dofus.network.enums.PartyTypeEnum;
import com.ankamagames.dofus.network.types.game.context.roleplay.party.PartyMemberInformations;
import com.github.blackrush.acara.EventBus;
import org.heat.world.roleplay.WorldHumanoidActor;

import java.util.Optional;
import java.util.stream.Stream;

public interface WorldGroup {
    EventBus getEventBus();

    int getPartyId();
    PartyTypeEnum getPartyType();

    WorldGroupMember getLeader();
    void abdicateLeader(WorldGroupMember newLeader);

    Invitation invite(WorldGroupMember inviter, WorldGroupMember guest);
    Optional<Invitation> findInvitation(int guestId);
    void update(WorldGroupMember member);
    void leave(WorldGroupMember member);
    void kick(WorldGroupMember kicker, WorldGroupMember member);

    Stream<WorldGroupMember> getMemberStream();
    Optional<WorldGroupMember> findMember(int memberId);

    default Stream<PartyMemberInformations> toPartyMemberInformations() {
        return getMemberStream().map(WorldGroupMember::toPartyMemberInformations);
    }

    public interface Invitation {
        WorldGroupGuest getGroupGuest();

        void accept();
        void refuse();

        default WorldHumanoidActor getGuest() {
            return getGroupGuest().getGuest();
        }

        default WorldGroupMember getInviter() {
            return getGroupGuest().getInviter();
        }
    }
}
