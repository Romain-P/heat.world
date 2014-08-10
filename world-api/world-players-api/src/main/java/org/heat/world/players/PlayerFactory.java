package org.heat.world.players;

import org.fungsi.concurrent.Future;
import org.heat.User;

public interface PlayerFactory {
    Future<Player> create(User user, String name, byte breed, boolean sex, int[] colors, int cosmeticId);
}
