package org.heat.world.players;

import com.ankamagames.dofus.datacenter.breeds.Breed;
import com.google.common.collect.ImmutableMap;
import lombok.Getter;
import lombok.Setter;
import org.heat.shared.IntPair;
import org.heat.world.metrics.GameStat;
import org.heat.world.metrics.GameStats;
import org.heat.world.metrics.RegularStat;

import java.util.Optional;

import static org.heat.world.metrics.GameStats.*;

public final class DefaultPlayerStatBook implements PlayerStatBook {

    private final Breed breed;

    private final ImmutableMap<GameStats<?>, GameStat> map;

    @Getter @Setter
    private int statsPoints;

    @Getter @Setter
    private int spellsPoints;

    public DefaultPlayerStatBook(Breed breed) {
        this.breed = breed;
        this.map = GameStats.newFilledMap();
    }

    public DefaultPlayerStatBook(Breed breed, int statsPoints, int spellsPoints, short strength, short vitality, short wisdom, short chance, short agility, short intelligence) {
        this(breed);

        this.statsPoints = statsPoints;
        this.spellsPoints = spellsPoints;

        get(STRENGTH).setBase(strength);
        get(VITALITY).setBase(vitality);
        get(WISDOM).setBase(wisdom);
        get(CHANCE).setBase(chance);
        get(AGILITY).setBase(agility);
        get(INTELLIGENCE).setBase(intelligence);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends GameStat> T get(GameStats<T> id) {
        // NOTE(Blackrush): Never null since we filled backing map with all existing id
        GameStat stat = map.get(id);
        return (T) stat;
    }

    @Override
    public <T extends GameStat> Optional<T> lookup(GameStats<T> id) {
        return Optional.of(get(id));
    }

    @Override
    public void plusStatsPoints(int statsPoints) {
        this.statsPoints += statsPoints;
    }

    @Override
    public void plusSpellsPoints(int spellsPoints) {
        this.spellsPoints += spellsPoints;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int upgradeStat(GameStats<RegularStat> id, int points) {
        if (statsPoints < points) {
            throw new IllegalArgumentException("not enough stats points (currently " + statsPoints + " but wanted " + points + ")");
        }

        RegularStat stat = get(id);
        long[][] statsPoints = Players.getStatsPoints(breed, id).get();

        IntPair boost = Players.upgrade(statsPoints, stat.getBase(), points);
        stat.plusBase((short) boost.first);
        this.statsPoints -= boost.second;
        return boost.first;
    }
}