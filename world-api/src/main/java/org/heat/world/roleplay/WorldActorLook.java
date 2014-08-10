package org.heat.world.roleplay;

import com.ankamagames.dofus.network.types.game.look.EntityLook;
import lombok.Value;
import lombok.experimental.Wither;

import java.util.stream.Stream;

@Value
@Wither
public final class WorldActorLook {
    final Bones bones;
    final short lookId;
    final short headId;
    final short scale;
    final int[] colors;

    public EntityLook toEntityLook() {
        return new EntityLook(
                bones.value,
                new short[] {lookId, headId}, // TODO(world): entity look skins
                colors,
                new short[] {scale},
                Stream.empty() // TODO(world): entity look subentities
        );
    }

    public enum Bones {
        STANDING(1),
        MOUNTING(639),
        ;

        public final short value;

        Bones(int value) {
            this.value = (short) value;
        }
    }

    public static int[] toIndexedColors(int[] colors) {
        // << index :: size(8), color :: size(24) >>
        int[] res = new int[colors.length];
        for (int i = 0; i < colors.length; i++) {
            res[i] = (((i + 1) & 0xFF) << 24) | (colors[i] & 0xFFFFFF);
        }
        return res;
    }
}
