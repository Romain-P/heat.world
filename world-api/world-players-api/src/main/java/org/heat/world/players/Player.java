package org.heat.world.players;

import com.ankamagames.dofus.datacenter.breeds.Breed;
import com.ankamagames.dofus.network.enums.CharacterInventoryPositionEnum;
import com.ankamagames.dofus.network.enums.DirectionsEnum;
import com.ankamagames.dofus.network.types.game.character.alignment.ActorAlignmentInformations;
import com.ankamagames.dofus.network.types.game.character.alignment.ActorExtendedAlignmentInformations;
import com.ankamagames.dofus.network.types.game.character.characteristic.CharacterCharacteristicsInformations;
import com.ankamagames.dofus.network.types.game.character.choice.CharacterBaseInformations;
import com.ankamagames.dofus.network.types.game.character.restriction.ActorRestrictionsInformations;
import com.ankamagames.dofus.network.types.game.context.roleplay.GameRolePlayCharacterInformations;
import com.ankamagames.dofus.network.types.game.context.roleplay.HumanInformations;
import com.github.blackrush.acara.EventBus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.heat.shared.stream.MoreCollectors;
import org.heat.world.items.WorldItem;
import org.heat.world.items.WorldItemType;
import org.heat.world.items.WorldItemWallet;
import org.heat.world.metrics.GameStats;
import org.heat.world.players.items.PlayerItemWallet;
import org.heat.world.players.metrics.PlayerExperience;
import org.heat.world.players.metrics.PlayerSpellBook;
import org.heat.world.players.metrics.PlayerStatBook;
import org.heat.world.players.shortcuts.PlayerShortcutBar;
import org.heat.world.roleplay.WorldActor;
import org.heat.world.roleplay.WorldActorLook;
import org.heat.world.roleplay.WorldHumanoidActor;
import org.heat.world.roleplay.environment.WorldMapPoint;
import org.heat.world.roleplay.environment.WorldPosition;
import org.heat.world.trading.impl.player.PlayerTrader;

import java.io.Serializable;
import java.time.Instant;
import java.util.Optional;
import java.util.stream.Stream;

import static com.ankamagames.dofus.network.enums.CharacterInventoryPositionEnum.*;

@RequiredArgsConstructor
@Getter
@Setter
@ToString
public class Player
        implements Serializable,
            WorldHumanoidActor,
            PlayerTrader
{
    EventBus eventBus;
    int id;
    int userId;
    String name;
    Breed breed;
    boolean sex;
    WorldActorLook look;
    WorldPosition position;
    PlayerExperience experience;
    PlayerStatBook stats;
    PlayerSpellBook spells;
    PlayerItemWallet wallet;
    PlayerShortcutBar shortcutBar;

    Instant lastUsedAt;

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

    @Override
    public WorldItemWallet getTraderBag() {
        return wallet;
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

    @SuppressWarnings({"SimplifiableIfStatement", "RedundantIfStatement"})
    public boolean canMoveItemTo(WorldItem item, CharacterInventoryPositionEnum to, int quantity) {
        /**
         * TODO(world/items): item movement validity
         * you cannot equip a pet if there is a mount
         */

        // we only worry if we want to equip an item
        if (to == INVENTORY_POSITION_NOT_EQUIPED) {
            return true;
        }

        // you cannot equip if target position is already taken
        if (wallet.findByPosition(to).findAny().isPresent()) {
            return false;
        }

        // you cannot equip a greater level item
        if (item.getTemplate().getLevel() > getExperience().getCurrentLevel()) {
            return false;
        }

        // this item type can not be moved here
        if (!item.getItemType().canBeMovedTo(to)) {
            return false;
        }

        // make sure we do not equip a ring twice
        if (item.getItemType() == WorldItemType.RING) {
            CharacterInventoryPositionEnum backwards = to == INVENTORY_POSITION_RING_LEFT
                    ? INVENTORY_POSITION_RING_RIGHT
                    : INVENTORY_POSITION_RING_LEFT;

            WorldItem otherRing = wallet.findByPosition(backwards).collect(MoreCollectors.uniqueOption()).orElse(null);

            if (otherRing != null && otherRing.getGid() == item.getGid()) {
                return false;
            }
        }

        // we want to equip only *one* item
        if (item.getItemType().isEquipment() && quantity != 1) {
            return false;
        }

        return true;
    }

    /**
     * Get max transportable weight. See http://dofuswiki.wikia.com/wiki/Characteristic#Pods_.28Carrying_Capacity.29
     *
     * <p>
     * This statistic determines the number of items you can carry. The base value is 1000.
     * Each profession level of the character gives +5 pods, and each level 100 profession gives an additional +1000 pods.
     * Strength also affects carrying capacity, at the rate of 5 pods per strength point.
     * Pods can also be obtained from Pods equipment.
     *
     * @return an integer
     */
    public int getMaxWeight() {
        return Players.BASE_TRANSPORTABLE_WEIGHT
                + stats.get(GameStats.STRENGTH).getSafeTotal() * 5
                + stats.get(GameStats.PODS).getTotal()
                // TODO(world/players): jobs affect transportable weight
                ;
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

    public static Optional<Player> asPlayer(WorldActor actor) {
        if (actor instanceof Player) {
            return Optional.of((Player) actor);
        }
        return Optional.empty();
    }
}
