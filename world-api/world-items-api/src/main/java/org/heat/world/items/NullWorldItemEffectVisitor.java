package org.heat.world.items;

import org.heat.world.items.effects.IntegerItemEffect;

public class NullWorldItemEffectVisitor<R> implements WorldItemEffectVisitor<R> {
    @Override
    public R visitInteger(IntegerItemEffect effect) {
        return null;
    }

    @Override
    public R otherwise(WorldItemEffect effect) {
        return null;
    }
}
