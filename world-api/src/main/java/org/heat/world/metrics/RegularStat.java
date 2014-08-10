package org.heat.world.metrics;

import com.ankamagames.dofus.network.types.game.character.characteristic.CharacterBaseCharacteristic;
import lombok.Data;
import org.heat.shared.Arithmetics;

@Data
public final class RegularStat implements GameStat {
    final GameStats<LimitStat> id;

    short base, objectsAndMountBonus, alignGiftBonus, contextModif;

    @Override
    public short getTotal() {
        return Arithmetics.addShorts(base, objectsAndMountBonus, alignGiftBonus, contextModif);
    }

    public void plusBase(short base) {
        this.base = Arithmetics.addShorts(this.base, base);
    }

    public void plusObjectsAndMountBonus(short objectsAndMountBonus) {
        this.objectsAndMountBonus = Arithmetics.addShorts(this.objectsAndMountBonus, objectsAndMountBonus);
    }

    public void plusAlignGiftBonus(short alignGiftBonus) {
        this.alignGiftBonus = Arithmetics.addShorts(this.alignGiftBonus, alignGiftBonus);
    }

    public void plusContextModif(short contextModif) {
        this.contextModif = Arithmetics.addShorts(this.contextModif, contextModif);
    }

    public CharacterBaseCharacteristic toCharacterBaseCharacteristic() {
        return new CharacterBaseCharacteristic(base, objectsAndMountBonus, alignGiftBonus, contextModif);
    }
}
