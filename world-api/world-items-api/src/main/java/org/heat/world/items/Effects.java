package org.heat.world.items;

import com.ankamagames.dofus.datacenter.effects.EffectInstance;
import com.ankamagames.dofus.datacenter.effects.instances.*;
import com.ankamagames.dofus.network.types.game.data.items.effects.*;
import org.heat.world.items.effects.FallbackItemEffect;
import org.heat.world.items.effects.IntegerItemEffect;

import java.util.concurrent.ThreadLocalRandom;

final class Effects {
    private Effects() {}

    public static WorldItemEffect fromObjectEffect(ObjectEffect effect) {
        if (effect instanceof ObjectEffectInteger) {
            return new IntegerItemEffect(effect.actionId, ((ObjectEffectInteger) effect).value);
        }

        return new FallbackItemEffect(effect);
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

        return new FallbackItemEffect(toObjectEffect(effect));
    }

    public static ObjectEffect toObjectEffect(EffectInstance effect) {
        if (effect instanceof EffectInstanceLadder) {
            return new ObjectEffectLadder(
                    (short) effect.getEffectId(),
                    (short) ((EffectInstanceLadder) effect).getMonsterFamilyId(),
                    (short) ((EffectInstanceLadder) effect).getMonsterCount()
            );
        }

        if (effect instanceof EffectInstanceCreature) {
            return new ObjectEffectCreature(
                    (short) effect.getEffectId(),
                    (short) ((EffectInstanceCreature) effect).getMonsterFamilyId()
            );
        }

        if (effect instanceof EffectInstanceDate) {
            return new ObjectEffectDate(
                    (short) effect.getEffectId(),
                    (short) ((EffectInstanceDate) effect).getYear(),
                    (short) ((EffectInstanceDate) effect).getMonth(),
                    (short) ((EffectInstanceDate) effect).getDay(),
                    (short) ((EffectInstanceDate) effect).getHour(),
                    (short) ((EffectInstanceDate) effect).getMinute()
            );
        }

        if (effect instanceof EffectInstanceDice) {
            short num = (short) ((EffectInstanceDice) effect).getDiceNum();
            short side = (short) ((EffectInstanceDice) effect).getDiceSide();
            short constt = (short) 0;
            if (side == 0) {
                constt = num;
                num = (short) 0;
            }
            return new ObjectEffectDice(
                    (short) effect.getEffectId(),
                    num,
                    side,
                    constt
            );
        }

        if (effect instanceof EffectInstanceDuration) {
            return new ObjectEffectDuration(
                    (short) effect.getEffectId(),
                    (short) ((EffectInstanceDuration) effect).getDays(),
                    (short) ((EffectInstanceDuration) effect).getHours(),
                    (short) ((EffectInstanceDuration) effect).getMinutes()
            );
        }
        if (effect instanceof EffectInstanceInteger) {
            return new ObjectEffectInteger(
                    (short) effect.getEffectId(),
                    (short) ((EffectInstanceInteger) effect).getValue()
            );
        }

        if (effect instanceof EffectInstanceMinMax) {
            return new ObjectEffectMinMax(
                    (short) effect.getEffectId(),
                    (short) ((EffectInstanceMinMax) effect).getMin(),
                    (short) ((EffectInstanceMinMax) effect).getMax()
            );
        }

        if (effect instanceof EffectInstanceMount) {
            return new ObjectEffectMount(
                    (short) effect.getEffectId(),
                    (int) ((EffectInstanceMount) effect).getMountId(),
                    ((EffectInstanceMount) effect).getDate(),
                    (short) ((EffectInstanceMount) effect).getModelId()
            );
        }

        if (effect instanceof EffectInstanceString) {
            return new ObjectEffectString(
                    (short) effect.getEffectId(),
                    ((EffectInstanceString) effect).getText()
            );
        }

        throw new IllegalArgumentException();
    }
}
