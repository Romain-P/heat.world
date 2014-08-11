package org.heat.world.players;

import com.ankamagames.dofus.datacenter.spells.Spell;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Stream;

public interface PlayerSpellBook {
    void add(Spell spell, OptionalInt position);

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
