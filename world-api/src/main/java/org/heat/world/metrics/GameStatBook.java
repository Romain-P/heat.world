package org.heat.world.metrics;

import java.util.Optional;

public interface GameStatBook {
    <T extends GameStat> Optional<T> lookup(GameStats<T> id);

    default <T extends GameStat> T get(GameStats<T> id) {
        return lookup(id).get();
    }
}
