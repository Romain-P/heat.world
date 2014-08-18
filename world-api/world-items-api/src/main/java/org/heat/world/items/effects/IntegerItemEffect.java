package org.heat.world.items.effects;

import com.ankamagames.dofus.network.types.game.data.items.effects.ObjectEffect;
import com.ankamagames.dofus.network.types.game.data.items.effects.ObjectEffectInteger;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.heat.world.items.WorldItemEffect;

@ToString(of = {"actionId", "value"})
@EqualsAndHashCode(of = {"actionId", "value"})
@RequiredArgsConstructor
public final class IntegerItemEffect implements WorldItemEffect {
    @Getter final short actionId;
    @Getter final short value;

    private ObjectEffect cache;
    @Override
    public ObjectEffect toObjectEffect() {
        if (cache == null) {
            cache = new ObjectEffectInteger(actionId, value);
        }
        return cache;
    }
}
