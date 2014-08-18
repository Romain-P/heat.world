package org.heat.world.players;

import java.util.List;
import java.util.Optional;

public interface PlayerRepository {
    Optional<Player> find(int id);
    List<Player> findByUserId(int userId);
    Optional<Player> findByName(String name);

    void create(Player player);
    void save(Player player);
    void remove(Player player);
}
