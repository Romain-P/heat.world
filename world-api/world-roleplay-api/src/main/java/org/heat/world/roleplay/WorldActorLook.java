package org.heat.world.roleplay;

import com.ankamagames.dofus.network.types.game.look.EntityLook;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.stream.Stream;

@Getter
@EqualsAndHashCode
@ToString
public final class WorldActorLook {
    final short bones;
    final short[] skins;
    final short[] scales;
    final int[] colors;

    public WorldActorLook(short bones, short[] skins, short[] scales, int[] colors) {
        this.bones = bones;
        this.skins = skins;
        this.scales = scales;
        this.colors = colors;
    }

    public WorldActorLook(short bones, short lookId, short headId, short scale, int[] colors) {
        this(bones, new short[] {lookId, headId}, new short[]{scale}, WorldActorLooks.toIndexedColors(colors));
    }

    public int getLookId() {
        if (skins.length <= 0) {
            throw new IllegalStateException();
        }
        return skins[0];
    }

    public int getHeadId() {
        if (skins.length <= 1) {
            throw new IllegalStateException();
        }
        return skins[1];
    }

    public int getScale() {
        if (scales.length != 1) {
            throw new IllegalStateException();
        }
        return scales[0];
    }

    public EntityLook toEntityLook() {
        return new EntityLook(
                bones,
                skins,
                colors,
                scales,
                Stream.empty() // TODO(world): entity look subentities
        );
    }

    public static final short
        STANDING_BONES = 1,
        MOUNTING_BONES = 639
        ;
}
