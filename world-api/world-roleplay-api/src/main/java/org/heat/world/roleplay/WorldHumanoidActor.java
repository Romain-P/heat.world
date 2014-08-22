package org.heat.world.roleplay;

import com.ankamagames.dofus.network.types.game.character.restriction.ActorRestrictionsInformations;
import com.ankamagames.dofus.network.types.game.context.roleplay.GameRolePlayHumanoidInformations;
import com.ankamagames.dofus.network.types.game.context.roleplay.HumanInformations;

public interface WorldHumanoidActor extends WorldNamedActor {
    int getActorUserId();
    boolean getActorSex();

    ActorRestrictionsInformations toActorRestrictionsInformations();
    HumanInformations toHumanInformations();

    @Override
    GameRolePlayHumanoidInformations toGameRolePlayActorInformations();
}
