package org.heat.world.groups;

import com.ankamagames.dofus.network.enums.PartyTypeEnum;
import com.ankamagames.dofus.network.types.game.context.roleplay.party.PartyGuestInformations;
import com.ankamagames.dofus.network.types.game.context.roleplay.party.PartyMemberInformations;
import com.github.blackrush.acara.EventBus;
import org.heat.world.roleplay.WorldHumanoidActor;

import java.util.Optional;
import java.util.stream.Stream;

public interface WorldGroup {
    EventBus getEventBus();

    int getGroupId();
    PartyTypeEnum getGroupType();

    WorldGroupMember getLeader();
    void abdicateLeader(WorldGroupMember newLeader);

    Invitation invite(WorldGroupMember inviter, WorldGroupMember guest);
    Optional<Invitation> findInvitation(int guestId);
    void update(WorldGroupMember member);
    void leave(WorldGroupMember member);
    void kick(WorldGroupMember kicker, WorldGroupMember member);

    Stream<WorldGroupMember> getMemberStream();
    Optional<WorldGroupMember> findMember(int memberId);

    Stream<WorldGroupGuest> getGuestStream();

    default Stream<PartyMemberInformations> toPartyMemberInformations() {
        return getMemberStream().map(WorldGroupMember::toPartyMemberInformations);
    }

    default Stream<PartyGuestInformations> toPartyGuestInformations() {
        return getGuestStream().map(WorldGroupGuest::toPartyGuestInformations);
    }

    public interface Invitation {
        public enum AckT { INSTANCE }
        public static final AckT Ack = AckT.INSTANCE;

        WorldGroup getGroup();
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
