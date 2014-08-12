package org.heat.world.items;

import com.ankamagames.dofus.datacenter.items.Item;
import com.ankamagames.dofus.datacenter.items.Weapon;
import com.ankamagames.dofus.network.enums.CharacterInventoryPositionEnum;
import com.ankamagames.dofus.network.types.game.data.items.ObjectItem;
import com.google.common.collect.ImmutableSet;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.fungsi.Either;

import java.util.function.Function;

import static java.util.Objects.requireNonNull;

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
@Getter
@RequiredArgsConstructor(staticName = "create")
@EqualsAndHashCode(of = {"uid", "version"})
public final class WorldItem {
    final int uid;
    final long version;
    final Item template;
    final ImmutableSet<WorldItemEffect> effects;
    final CharacterInventoryPositionEnum position;
    final int quantity;

    // backdoors
    WorldItem copy(Item template, ImmutableSet<WorldItemEffect> effects, CharacterInventoryPositionEnum position, int quantity) {
        return create(uid, version + 1, template, effects, position, quantity);
    }

    WorldItem withUid(int uid) {
        return create(uid, version + 1, template, effects, position, quantity);
    }

    WorldItem withNewVersion() {
        return copy(template, effects, position, quantity);
    }

    // mutators
    public WorldItem withPosition(CharacterInventoryPositionEnum position) {
        requireNonNull(position, "position");
        return copy(template, effects, position, quantity);
    }

    public WorldItem withQuantity(int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("quantity must be positive or zero");
        }
        return copy(template, effects, position, quantity);
    }

    public WorldItem withEffects(ImmutableSet<WorldItemEffect> effects) {
        requireNonNull(effects, "effects");
        return copy(template, effects, position, quantity);
    }

    // shorthands
    public short getGid() {
        return (short) getTemplate().getId();
    }

    public boolean isWeapon() {
        return getTemplate() instanceof Weapon;
    }

    public Either<Item, Weapon> getItemOrWeapon() {
        return isWeapon() ? Either.right(((Weapon) getTemplate())) : Either.left(getTemplate());
    }

    public WorldItem plusQuantity(int quantity) {
        return withQuantity(getQuantity() + quantity);
    }

    public WorldItem mapEffects(Function<WorldItemEffect, WorldItemEffect> fn) {
        ImmutableSet.Builder<WorldItemEffect> newEffects = ImmutableSet.builder();
        for (WorldItemEffect effect : getEffects()) {
            newEffects.add(fn.apply(effect));
        }

        return withEffects(newEffects.build());
    }

    public ObjectItem toObjectItem() {
        return new ObjectItem(
                getPosition().value,
                getGid(),
                getEffects().stream().map(WorldItemEffect::toObjectEffect),
                getUid(),
                getQuantity()
        );
    }
}
