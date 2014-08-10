package org.heat.world.players;

import org.heat.shared.database.MutableRepository;

import java.util.List;
import java.util.Optional;

public interface PlayerRepository extends MutableRepository<Player> {
    Optional<Player> find(int id);
    List<Player> findByUserId(int userId);
    Optional<Player> findByName(String name);

    @Override
    default Optional<Player> find(long id) {
        return find((int) id);
    }
}
