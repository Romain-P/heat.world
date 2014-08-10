package org.heat.world.players;

import com.ankamagames.dofus.datacenter.breeds.Breed;
import com.ankamagames.dofus.network.enums.DirectionsEnum;
import com.ankamagames.dofus.network.types.game.character.alignment.ActorAlignmentInformations;
import com.ankamagames.dofus.network.types.game.character.alignment.ActorExtendedAlignmentInformations;
import com.ankamagames.dofus.network.types.game.character.characteristic.CharacterCharacteristicsInformations;
import com.ankamagames.dofus.network.types.game.character.choice.CharacterBaseInformations;
import com.ankamagames.dofus.network.types.game.character.restriction.ActorRestrictionsInformations;
import com.ankamagames.dofus.network.types.game.context.roleplay.GameRolePlayCharacterInformations;
import com.ankamagames.dofus.network.types.game.context.roleplay.HumanInformations;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.heat.world.roleplay.WorldActorLook;
import org.heat.world.roleplay.WorldHumanoidActor;
import org.heat.world.roleplay.environment.WorldMapPoint;
import org.heat.world.roleplay.environment.WorldPosition;

import java.io.Serializable;
import java.util.stream.Stream;

@RequiredArgsConstructor
@Getter
@Setter
@ToString
public class Player
        implements Serializable,
            WorldHumanoidActor
{
    int id;
    int userId;
    String name;
    Breed breed;
    boolean sex;
    WorldActorLook look;
    WorldPosition position;
    PlayerExperience experience;
    PlayerStatBook stats;

    // lombok auto-generates a #isSex() which is invalid here
    public boolean getSex() {
        return sex;
    }

    public void moveTo(WorldMapPoint point, DirectionsEnum dir) {
        setPosition(getPosition().moveTo(point, dir));
    }

    public CharacterBaseInformations toCharacterBaseInformations() {
        return new CharacterBaseInformations(
                id,
                (short) experience.getCurrentLevel(),
                name,
                look.toEntityLook(),
                (byte) breed.getId(),
                sex
        );
    }

    public ActorAlignmentInformations toActorAlignmentInformations() {
        // TODO(world/players): alignment
        return new ActorAlignmentInformations();
    }

    @Override
    public int getActorId() {
        return id;
    }

    @Override
    public int getActorUserId() {
        return userId;
    }

    @Override
    public String getActorName() {
        return name;
    }

    @Override
    public WorldActorLook getActorLook() {
        return look;
    }

    @Override
    public WorldPosition getActorPosition() {
        return position;
    }

    @Override
    public boolean getActorSex() {
        return sex;
    }

    @Override
    public ActorRestrictionsInformations toActorRestrictionsInformations() {
        // TODO(world/players): actor restrictions
        return new ActorRestrictionsInformations();
    }

    @Override
    public HumanInformations toHumanInformations() {
        // TODO(world/players): human options
        return new HumanInformations(toActorRestrictionsInformations(), sex, Stream.empty());
    }

    @Override
    public GameRolePlayCharacterInformations toGameRolePlayActorInformations() {
        return new GameRolePlayCharacterInformations(
                id,
                look.toEntityLook(),
                position.toEntityDispositionInformations(),
                name,
                toHumanInformations(),
                userId,
                toActorAlignmentInformations()
        );
    }

    public CharacterCharacteristicsInformations toCharacterCharacteristicsInformations() {
        CharacterCharacteristicsInformations res = new CharacterCharacteristicsInformations();

        // TODO(world/players): alignment
        res.alignmentInfos = new ActorExtendedAlignmentInformations();
        // TODO(world/players): spell modifications
        res.spellModifications = Stream.empty();

        res.experience = experience.getCurrent();
        res.experienceLevelFloor = experience.getStep().getTop();
        res.experienceNextLevelFloor = experience.getStep().getNext().orElse(experience.getStep()).getTop();

        res.statsPoints = stats.getStatsPoints();
        res.spellsPoints = stats.getSpellsPoints();
        Players.populateCharacterCharacteristicsInformations(stats, res);

        return res;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Player player = (Player) o;
        return id == player.id;

    }

    @Override
    public int hashCode() {
        return id;
    }
}
