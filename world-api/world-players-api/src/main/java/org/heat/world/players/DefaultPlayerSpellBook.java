package org.heat.world.players;

import com.ankamagames.dofus.datacenter.breeds.Breed;
import com.ankamagames.dofus.datacenter.spells.Spell;
import org.heat.data.Datacenter;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.stream.Stream;

public final class DefaultPlayerSpellBook implements PlayerSpellBook {

    private final List<PlayerSpell> spells;

    private DefaultPlayerSpellBook(List<PlayerSpell> spells) {
        this.spells = spells;
    }

    public static DefaultPlayerSpellBook create(List<PlayerSpell> spells) {
        return new DefaultPlayerSpellBook(new ArrayList<>(spells));
    }

    public static DefaultPlayerSpellBook create(Datacenter datacenter, Breed breed, int actualLevel) {
        return new DefaultPlayerSpellBook(Players.buildDefaultBreedSpells(datacenter, breed, actualLevel));
    }

    @Override
    public void add(Spell spell, OptionalInt position) {
        spells.add(PlayerSpell.create(spell, Players.NEW_SPELL_DEFAULT_MIN_LEVEL, position));
    }

    @Override
    public Stream<PlayerSpell> getSpellStream() {
        return spells.stream();
    }
}
