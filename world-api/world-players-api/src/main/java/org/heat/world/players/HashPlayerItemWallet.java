package org.heat.world.players;

import lombok.Getter;
import org.heat.world.items.HashItemBag;

public final class HashPlayerItemWallet extends HashItemBag implements PlayerItemWallet {
    @Getter int kamas;

    @Override
    public void setKamas(int kamas) {
        if (kamas < 0) {
            throw new IllegalArgumentException("an amount of kamas is always positive or zero");
        }
        this.kamas = kamas;
    }
}
