package org.heat.world.players;

import lombok.Getter;
import lombok.Setter;
import org.heat.world.items.HashItemBag;

public final class HashPlayerItemWallet extends HashItemBag implements PlayerItemWallet {
    @Getter @Setter int kamas;
}
