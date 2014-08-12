package org.heat.world.items;

import com.ankamagames.dofus.datacenter.items.Item;
import com.ankamagames.dofus.datacenter.items.Weapon;
import com.ankamagames.dofus.network.enums.CharacterInventoryPositionEnum;
import com.ankamagames.dofus.network.types.game.data.items.ObjectItem;
import org.fungsi.Either;

import java.util.stream.Stream;

/**
 * {@link org.heat.world.items.WorldItem} is an immutable data class.
 *
 * <p>
 * It has two identifiers :
 * <ol>
 *     <li>UID, standing for User IDentifier, a unique identifier across all items</li>
 *     <li>GID, standing for Group IDentifier, an identifier referencing template's id</li>
 * </ol>
 */
public interface WorldItem {
    // constant properties
    long getVersion();
    int getUid();
    Item getTemplate();
    Stream<WorldItemEffect> getEffectStream();

    // mutable properties
    CharacterInventoryPositionEnum getPosition();
    int getQuantity();

    WorldItem withNewVersion();
    WorldItem withPosition(CharacterInventoryPositionEnum position);
    WorldItem withQuantity(int quantity);

    // shorthands
    default short getGid() {
        return (short) getTemplate().getId();
    }

    default ObjectItem toObjectItem() {
        return new ObjectItem(
                getPosition().value,
                getGid(),
                getEffectStream().map(WorldItemEffect::toObjectEffect),
                getUid(),
                getQuantity()
        );
    }

    default boolean isWeapon() {
        return getTemplate() instanceof Weapon;
    }

    default Either<Item, Weapon> getItemOrWeapon() {
        return isWeapon()
                ? Either.right((Weapon) getTemplate())
                : Either.left(getTemplate());
    }

    default WorldItem plusQuantity(int quantity) {
        return withQuantity(getQuantity() + quantity);
    }
}
