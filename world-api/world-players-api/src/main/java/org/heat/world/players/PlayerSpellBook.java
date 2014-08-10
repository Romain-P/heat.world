package org.heat.world.players;

import java.util.Optional;
import java.util.stream.Stream;

public interface PlayerSpellBook {
    Stream<PlayerSpell> getSpellStream();

    default Optional<PlayerSpell> findById(int id) {
        return getSpellStream().filter(x -> x.getId() == id).findAny();
    }

    default Optional<PlayerSpell> findByPosition(int position) {
        return getSpellStream().filter(x -> x.hasPosition(position)).findAny();
    }

    default Stream<PlayerSpell> getWithoutPosition() {
        return getSpellStream().filter(x -> !x.getPosition().isPresent());
    }

    default void resetAll() {
        getSpellStream().forEach(PlayerSpell::resetLevel);
    }
}
