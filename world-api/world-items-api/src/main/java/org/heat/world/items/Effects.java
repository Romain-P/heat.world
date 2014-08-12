package org.heat.world.items;

import com.ankamagames.dofus.datacenter.effects.EffectInstance;
import com.ankamagames.dofus.datacenter.effects.instances.EffectInstanceDice;
import com.ankamagames.dofus.datacenter.effects.instances.EffectInstanceInteger;
import com.ankamagames.dofus.network.types.game.data.items.effects.ObjectEffect;
import com.ankamagames.dofus.network.types.game.data.items.effects.ObjectEffectInteger;
import org.heat.world.items.effects.IntegerItemEffect;

import java.util.concurrent.ThreadLocalRandom;

final class Effects {
    private Effects() {}

    public static WorldItemEffect fromObjectEffect(ObjectEffect effect) {
        if (effect instanceof ObjectEffectInteger) {
            return new IntegerItemEffect(effect.actionId, ((ObjectEffectInteger) effect).value);
        }

        // TODO(world/items): create effect from ObjectEffect
        throw new UnsupportedOperationException("not implemented");
    }

    public static WorldItemEffect fromEffectInstance(EffectInstance effect) {
        if (effect instanceof EffectInstanceDice) {
            EffectInstanceDice dice = (EffectInstanceDice) effect;

            short value;
            if (dice.getDiceSide() == 0) {
                value = (short) dice.getDiceNum();
            } else {
                value = (short) ThreadLocalRandom.current().nextInt((int) dice.getDiceNum(), (int) dice.getDiceSide());
            }

            return new IntegerItemEffect((short) effect.getEffectId(), value);
        }

        if (effect instanceof EffectInstanceInteger) {
            return new IntegerItemEffect((short) effect.getEffectId(), (short) ((EffectInstanceInteger) effect).getValue());
        }

        // TODO(world/items): create effect from EffectInstance
        throw new UnsupportedOperationException("not implemented");
    }
}
