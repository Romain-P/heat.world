package org.heat.world.players.items;

import org.heat.world.items.WorldItemWallet;

public interface PlayerItemWallet extends WorldItemWallet {
    default int getWeight() {
        return getItemStream()
                .mapToInt(item -> (int) item.getTemplate().getRealWeight())
                .reduce(0, Integer::sum);
    }
}
